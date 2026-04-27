package main

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"io"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
	"testing"
	"time"
)

// TestDaemonHandshakeIntegration builds the shim, places it next to a
// `jars/` dir containing the latest jar-with-dependencies, and checks that
// `kapi-okapi-bridge daemon` emits a parsable handshake JSON line.
//
// Skipped when:
//   - $OKAPI_BRIDGE_INTEGRATION is not "1" (lets CI opt in deliberately)
//   - `java` is not on PATH (no JRE to run with)
//   - no jar-with-dependencies is built under okapi-releases/*/target/
func TestDaemonHandshakeIntegration(t *testing.T) {
	if os.Getenv("OKAPI_BRIDGE_INTEGRATION") != "1" {
		t.Skip("set OKAPI_BRIDGE_INTEGRATION=1 to run")
	}
	if _, err := exec.LookPath("java"); err != nil {
		t.Skip("java not on PATH")
	}

	repoRoot, err := findRepoRoot()
	if err != nil {
		t.Skipf("repo root not found: %v", err)
	}

	jar, err := newestJar(filepath.Join(repoRoot, "okapi-releases"))
	if err != nil {
		t.Skipf("no jar-with-dependencies built; run `make build V=...` first: %v", err)
	}

	// Build the shim into a tempdir laid out as the v2 tarball expects.
	stage := t.TempDir()
	jarDir := filepath.Join(stage, "jars")
	if err := os.MkdirAll(jarDir, 0o755); err != nil {
		t.Fatalf("mkdir: %v", err)
	}
	stagedJar := filepath.Join(jarDir, "neokapi-bridge-jar-with-dependencies.jar")
	if err := copyFile(jar, stagedJar); err != nil {
		t.Fatalf("copy jar: %v", err)
	}

	shimBin := filepath.Join(stage, "kapi-okapi-bridge")
	build := exec.Command("go", "build",
		"-o", shimBin,
		"-ldflags", "-X main.Version=integration-test",
		"./cmd/shim",
	)
	build.Dir = repoRoot
	build.Stderr = os.Stderr
	build.Stdout = os.Stderr
	if err := build.Run(); err != nil {
		t.Fatalf("build shim: %v", err)
	}

	ctx, cancel := context.WithTimeout(context.Background(), 60*time.Second)
	defer cancel()

	cmd := exec.CommandContext(ctx, shimBin, "daemon")
	cmd.Stderr = os.Stderr

	stdout, err := cmd.StdoutPipe()
	if err != nil {
		t.Fatalf("pipe: %v", err)
	}
	if err := cmd.Start(); err != nil {
		t.Fatalf("start: %v", err)
	}

	// Read the first line only — that's the handshake.
	br := newLineReader(stdout)
	line, err := br.readLineWithin(45 * time.Second)
	if err != nil {
		_ = cmd.Process.Kill()
		_ = cmd.Wait()
		t.Fatalf("read handshake line: %v", err)
	}
	t.Logf("handshake line: %s", line)

	var hs handshakePayload
	if err := json.Unmarshal([]byte(strings.TrimSpace(line)), &hs); err != nil {
		_ = cmd.Process.Kill()
		_ = cmd.Wait()
		t.Fatalf("handshake not JSON: %v: %q", err, line)
	}
	if hs.Version != "integration-test" {
		t.Errorf("version = %q, want integration-test", hs.Version)
	}
	if hs.Socket == "" {
		t.Errorf("socket empty")
	} else if _, err := os.Stat(hs.Socket); err != nil {
		t.Errorf("socket %q not present: %v", hs.Socket, err)
	}

	// Drain stdout so the JVM doesn't block on a full pipe.
	go io.Copy(io.Discard, stdout)

	// Send SIGTERM and ensure the shim exits cleanly within a few seconds.
	if err := cmd.Process.Signal(os.Interrupt); err != nil {
		t.Fatalf("interrupt: %v", err)
	}
	done := make(chan error, 1)
	go func() { done <- cmd.Wait() }()
	select {
	case err := <-done:
		if err != nil {
			var ee *exec.ExitError
			if errors.As(err, &ee) {
				// Signaled exits are acceptable.
				return
			}
			t.Errorf("shim wait: %v", err)
		}
	case <-time.After(15 * time.Second):
		_ = cmd.Process.Kill()
		t.Errorf("shim did not exit within 15s of SIGINT")
	}

	// Socket file should be cleaned up.
	if _, err := os.Stat(hs.Socket); err == nil {
		t.Errorf("socket %q not cleaned up after exit", hs.Socket)
	}
}

// findRepoRoot walks up from the current working directory looking for go.mod.
func findRepoRoot() (string, error) {
	wd, err := os.Getwd()
	if err != nil {
		return "", err
	}
	for {
		if _, err := os.Stat(filepath.Join(wd, "go.mod")); err == nil {
			return wd, nil
		}
		parent := filepath.Dir(wd)
		if parent == wd {
			return "", errors.New("no go.mod ancestor")
		}
		wd = parent
	}
}

// newestJar returns the newest path matching okapi-releases/*/target/
// neokapi-bridge-*-jar-with-dependencies.jar.
func newestJar(releasesDir string) (string, error) {
	matches, _ := filepath.Glob(filepath.Join(releasesDir, "*", "target", "neokapi-bridge-*-jar-with-dependencies.jar"))
	if len(matches) == 0 {
		return "", errors.New("none found")
	}
	var newest string
	var newestMtime time.Time
	for _, m := range matches {
		fi, err := os.Stat(m)
		if err != nil {
			continue
		}
		if fi.ModTime().After(newestMtime) {
			newestMtime = fi.ModTime()
			newest = m
		}
	}
	if newest == "" {
		return "", errors.New("no readable jar")
	}
	return newest, nil
}

func copyFile(src, dst string) error {
	in, err := os.Open(src)
	if err != nil {
		return err
	}
	defer in.Close()
	out, err := os.Create(dst)
	if err != nil {
		return err
	}
	if _, err := io.Copy(out, in); err != nil {
		_ = out.Close()
		return err
	}
	return out.Close()
}

// lineReader drains a reader one line at a time with timeouts so test
// failures don't hang the suite forever.
type lineReader struct {
	r     io.Reader
	buf   bytes.Buffer
	chunk []byte
}

func newLineReader(r io.Reader) *lineReader {
	return &lineReader{r: r, chunk: make([]byte, 1)}
}

func (lr *lineReader) readLineWithin(d time.Duration) (string, error) {
	deadline := time.Now().Add(d)
	for time.Now().Before(deadline) {
		n, err := lr.r.Read(lr.chunk)
		if n > 0 {
			b := lr.chunk[0]
			if b == '\n' {
				out := lr.buf.String()
				lr.buf.Reset()
				return out, nil
			}
			lr.buf.WriteByte(b)
		}
		if err != nil {
			return lr.buf.String(), err
		}
	}
	return lr.buf.String(), errors.New("deadline exceeded")
}
