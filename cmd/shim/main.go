// Command kapi-okapi-bridge is the Go shim that fronts the Okapi Java bridge
// in the v2 manifest-driven plugin model (issue neokapi/neokapi#438).
//
// kapi launches this binary as a Mode-C daemon. The shim is responsible for:
//
//  1. Locating a JRE (env override → bundled ./jre → $PATH).
//  2. Picking a unique Unix-domain socket path under $TMPDIR.
//  3. Spawning the JVM with NEOKAPI_BRIDGE_SOCKET set, classpath set to the
//     bundled jar, and main class neokapi.bridge.OkapiBridgeServer.
//  4. Reading the JVM's first stdout line — OkapiBridgeServer prints the
//     socket address on the first line — and verifying it matches the path
//     we requested.
//  5. Emitting the v2 canonical handshake JSON line on the shim's own stdout:
//     {"socket":"<path>","version":"<okapi-version>"}.
//  6. Forwarding the JVM's remaining stdout/stderr to the shim's stdout/stderr
//     so kapi sees subsequent log output.
//  7. Waiting for SIGTERM/SIGINT (or for the JVM to exit), then cleaning up
//     the socket file.
//
// Subcommands:
//
//	kapi-okapi-bridge daemon   — start the JVM and emit the handshake.
//	kapi-okapi-bridge version  — print the bundled Okapi version.
//	kapi-okapi-bridge --help   — print usage.
package main

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"os"
	"os/exec"
	"os/signal"
	"path/filepath"
	"runtime"
	"strings"
	"syscall"
)

// Version is the bundled Okapi Framework version. Overridden at build time
// with -ldflags "-X main.Version=<okapi-version>".
var Version = "0.0.0-dev"

// jarRelativePath is the path of the bundled jar relative to the directory
// containing the shim binary.
const jarRelativePath = "jars/neokapi-bridge-jar-with-dependencies.jar"

// jvmMainClass is the FQCN of the bridge's main class inside the bundled jar.
const jvmMainClass = "neokapi.bridge.OkapiBridgeServer"

// startupBufBytes is the cap on bytes we will buffer when reading the JVM's
// first line. Cheap defence against runaway output before the handshake.
const startupBufBytes = 64 * 1024

func main() {
	if err := run(os.Args[1:]); err != nil {
		fmt.Fprintln(os.Stderr, "kapi-okapi-bridge:", err)
		os.Exit(1)
	}
}

func run(args []string) error {
	if len(args) == 0 {
		printUsage(os.Stderr)
		return errors.New("missing subcommand")
	}
	switch args[0] {
	case "daemon":
		return runDaemon(args[1:])
	case "version", "--version", "-v":
		fmt.Println(Version)
		return nil
	case "help", "--help", "-h":
		printUsage(os.Stdout)
		return nil
	default:
		printUsage(os.Stderr)
		return fmt.Errorf("unknown subcommand %q", args[0])
	}
}

func printUsage(w io.Writer) {
	fmt.Fprintln(w, "Usage: kapi-okapi-bridge <subcommand>")
	fmt.Fprintln(w)
	fmt.Fprintln(w, "Subcommands:")
	fmt.Fprintln(w, "  daemon    Start the bridge JVM and emit a Mode-C handshake on stdout.")
	fmt.Fprintln(w, "  version   Print the bundled Okapi version.")
	fmt.Fprintln(w, "  help      Print this message.")
	fmt.Fprintln(w)
	fmt.Fprintln(w, "Environment:")
	fmt.Fprintln(w, "  NEOKAPI_JRE_HOME   Override JRE root directory (expects bin/java inside).")
	fmt.Fprintln(w, "  NEOKAPI_BRIDGE_SOCKET")
	fmt.Fprintln(w, "                     Override the Unix-socket path the JVM will bind.")
	fmt.Fprintln(w, "  KAPI_BRIDGE_HEAP   JVM max heap (e.g. 8g). Default: 16g.")
}

// runDaemon implements the `daemon` subcommand.
func runDaemon(_ []string) error {
	binDir, err := executableDir()
	if err != nil {
		return fmt.Errorf("locate self: %w", err)
	}

	javaBin, err := findJava(binDir, lookupEnv, statFile, lookPath)
	if err != nil {
		return err
	}

	jarPath := filepath.Join(binDir, jarRelativePath)
	if _, err := statFile(jarPath); err != nil {
		return fmt.Errorf("bundled jar not found at %s: %w", jarPath, err)
	}

	socketPath := os.Getenv("NEOKAPI_BRIDGE_SOCKET")
	if socketPath == "" {
		socketPath = defaultSocketPath(os.Getpid())
	}

	// Best-effort: remove any stale socket left over from a previous crash.
	_ = os.Remove(socketPath)

	heap := os.Getenv("KAPI_BRIDGE_HEAP")
	if heap == "" {
		heap = "16g"
	}

	jvmArgs := []string{
		"-Xmx" + heap,
		// Without this, DefaultChannelId init takes ~5s on macOS.
		"-Dio.netty.machineId=00:00:00:00:00:01",
		"-cp", jarPath,
		jvmMainClass,
	}

	ctx, cancel := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer cancel()

	cmd := exec.CommandContext(ctx, javaBin, jvmArgs...)
	cmd.Env = append(os.Environ(), "NEOKAPI_BRIDGE_SOCKET="+socketPath)
	cmd.Stderr = os.Stderr // forward JVM stderr verbatim

	stdout, err := cmd.StdoutPipe()
	if err != nil {
		return fmt.Errorf("pipe jvm stdout: %w", err)
	}

	if err := cmd.Start(); err != nil {
		return fmt.Errorf("start jvm: %w", err)
	}

	// Read the JVM's first stdout line — OkapiBridgeServer.main writes the
	// socket address on a single line and flushes before continuing.
	jvmAnnounce, err := readFirstLine(stdout, startupBufBytes)
	if err != nil {
		_ = cmd.Process.Kill()
		_ = cmd.Wait()
		return fmt.Errorf("read jvm announce: %w", err)
	}
	jvmAnnounce = strings.TrimSpace(jvmAnnounce)
	if jvmAnnounce != socketPath {
		_ = cmd.Process.Kill()
		_ = cmd.Wait()
		return fmt.Errorf("jvm announced socket %q, expected %q", jvmAnnounce, socketPath)
	}

	// Emit the v2 canonical handshake on the shim's stdout.
	if err := writeHandshake(os.Stdout, socketPath, Version); err != nil {
		_ = cmd.Process.Kill()
		_ = cmd.Wait()
		return fmt.Errorf("write handshake: %w", err)
	}

	// Forward the rest of the JVM's stdout to ours. This is best-effort —
	// when the JVM exits, the copy returns and we proceed to cleanup.
	copyDone := make(chan struct{})
	go func() {
		_, _ = io.Copy(os.Stdout, stdout)
		close(copyDone)
	}()

	waitErr := cmd.Wait()
	<-copyDone

	// Always try to remove the socket file. Ignore errors — kapi may have
	// removed it, or the JVM may have. Stale sockets are surprising but
	// harmless because the path embeds our pid.
	_ = os.Remove(socketPath)

	if waitErr != nil {
		// Context-cancelled exits look like signal-killed processes; treat
		// those as clean shutdowns since we initiated them.
		if ctx.Err() != nil {
			return nil
		}
		var exitErr *exec.ExitError
		if errors.As(waitErr, &exitErr) {
			return fmt.Errorf("jvm exited %d", exitErr.ExitCode())
		}
		return fmt.Errorf("jvm wait: %w", waitErr)
	}
	return nil
}

// handshakePayload is the v2 canonical handshake JSON the shim emits on its
// stdout's first line. The fields list is declared in manifest.json under
// daemon.handshake.fields.
type handshakePayload struct {
	Socket  string `json:"socket"`
	Version string `json:"version"`
}

func writeHandshake(w io.Writer, socket, version string) error {
	payload, err := json.Marshal(handshakePayload{Socket: socket, Version: version})
	if err != nil {
		return err
	}
	if _, err := w.Write(payload); err != nil {
		return err
	}
	if _, err := w.Write([]byte{'\n'}); err != nil {
		return err
	}
	if f, ok := w.(*os.File); ok {
		_ = f.Sync()
	}
	return nil
}

// readFirstLine reads up to a newline from r, capped at maxBytes. Returns
// the line without its trailing newline.
func readFirstLine(r io.Reader, maxBytes int) (string, error) {
	buf := make([]byte, 0, 256)
	one := make([]byte, 1)
	for len(buf) < maxBytes {
		n, err := r.Read(one)
		if n == 1 {
			if one[0] == '\n' {
				return string(buf), nil
			}
			buf = append(buf, one[0])
		}
		if err != nil {
			if err == io.EOF && len(buf) > 0 {
				return string(buf), nil
			}
			return string(buf), err
		}
	}
	return string(buf), fmt.Errorf("first line exceeded %d bytes", maxBytes)
}

// defaultSocketPath builds a per-PID Unix-socket path under $TMPDIR. The path
// must stay under the kernel's sun_path limit (104 on macOS, 108 on Linux),
// so we keep the basename short.
func defaultSocketPath(pid int) string {
	dir := os.Getenv("TMPDIR")
	if dir == "" {
		dir = os.TempDir()
	}
	// Trim trailing slash that some platforms add to TMPDIR.
	dir = strings.TrimRight(dir, string(os.PathSeparator))
	return filepath.Join(dir, fmt.Sprintf("kob-%d.sock", pid))
}

// executableDir returns the directory holding the running binary, resolving
// any symlinks. Used to anchor lookups for the bundled JRE and jar.
func executableDir() (string, error) {
	exe, err := os.Executable()
	if err != nil {
		return "", err
	}
	resolved, err := filepath.EvalSymlinks(exe)
	if err != nil {
		// EvalSymlinks fails on some Go test binaries; fall back to the
		// raw path which is fine when there is no symlink in the chain.
		resolved = exe
	}
	return filepath.Dir(resolved), nil
}

// findJava resolves the JRE binary path. Resolution order:
//
//  1. NEOKAPI_JRE_HOME (expects bin/java inside).
//  2. <binDir>/jre/bin/java — bundled JRE shipped alongside the shim.
//  3. JAVA_HOME (expects bin/java inside).
//  4. java on PATH.
//
// The lookupEnv, stat and lookPath functions are injected to make the
// resolver testable.
func findJava(
	binDir string,
	getenv func(string) string,
	stat func(string) (os.FileInfo, error),
	pathLookup func(string) (string, error),
) (string, error) {
	javaName := "java"
	if runtime.GOOS == "windows" {
		javaName = "java.exe"
	}

	if home := getenv("NEOKAPI_JRE_HOME"); home != "" {
		candidate := filepath.Join(home, "bin", javaName)
		if _, err := stat(candidate); err == nil {
			return candidate, nil
		}
	}

	bundled := filepath.Join(binDir, "jre", "bin", javaName)
	if _, err := stat(bundled); err == nil {
		return bundled, nil
	}

	if home := getenv("JAVA_HOME"); home != "" {
		candidate := filepath.Join(home, "bin", javaName)
		if _, err := stat(candidate); err == nil {
			return candidate, nil
		}
	}

	if path, err := pathLookup(javaName); err == nil {
		return path, nil
	}

	return "", errors.New("no JRE found: set NEOKAPI_JRE_HOME, install a bundled jre/, set JAVA_HOME, or put java on PATH")
}

// lookupEnv, statFile and lookPath are the production wiring for findJava.
// Tests substitute fakes.
func lookupEnv(k string) string                  { return os.Getenv(k) }
func statFile(p string) (os.FileInfo, error)     { return os.Stat(p) }
func lookPath(name string) (string, error)       { return exec.LookPath(name) }
