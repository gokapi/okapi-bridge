package main

import (
	"bytes"
	"encoding/json"
	"errors"
	"io"
	"os"
	"strings"
	"testing"
	"time"
)

// fakeFileInfo is a stand-in for os.FileInfo. We only need it for the not-nil
// signal so the methods can stay no-ops.
type fakeFileInfo struct{}

func (fakeFileInfo) Name() string      { return "java" }
func (fakeFileInfo) Size() int64       { return 0 }
func (fakeFileInfo) Mode() os.FileMode { return 0 }
func (fakeFileInfo) ModTime() time.Time { return time.Time{} }
func (fakeFileInfo) IsDir() bool       { return false }
func (fakeFileInfo) Sys() any          { return nil }

// realStat is just a wrapper that satisfies the type while always erroring.
// Tests construct their own fakeStat to drive the resolver.

type findJavaCase struct {
	name        string
	env         map[string]string
	existing    map[string]bool // file paths that "exist" for stat
	pathFound   string          // result for exec.LookPath; "" → not found
	pathErr     error           // err from exec.LookPath; only used when pathFound is ""
	binDir      string
	wantPath    string
	wantErrSub  string // substring expected in error message; "" → expect no error
}

func TestFindJava(t *testing.T) {
	t.Parallel()

	cases := []findJavaCase{
		{
			name:     "NEOKAPI_JRE_HOME wins",
			env:      map[string]string{"NEOKAPI_JRE_HOME": "/opt/myjre"},
			existing: map[string]bool{"/opt/myjre/bin/java": true, "/install/jre/bin/java": true},
			binDir:   "/install",
			wantPath: "/opt/myjre/bin/java",
		},
		{
			name:     "bundled jre falls through when env empty",
			env:      map[string]string{},
			existing: map[string]bool{"/install/jre/bin/java": true},
			binDir:   "/install",
			wantPath: "/install/jre/bin/java",
		},
		{
			name:     "JAVA_HOME after bundled missing",
			env:      map[string]string{"JAVA_HOME": "/sys/jdk"},
			existing: map[string]bool{"/sys/jdk/bin/java": true},
			binDir:   "/install",
			wantPath: "/sys/jdk/bin/java",
		},
		{
			name:      "PATH lookup last resort",
			env:       map[string]string{},
			existing:  map[string]bool{},
			pathFound: "/usr/bin/java",
			binDir:    "/install",
			wantPath:  "/usr/bin/java",
		},
		{
			name:       "all sources fail",
			env:        map[string]string{},
			existing:   map[string]bool{},
			pathErr:    errors.New("not found"),
			binDir:     "/install",
			wantErrSub: "no JRE found",
		},
		{
			name:     "NEOKAPI_JRE_HOME missing falls through to bundled",
			env:      map[string]string{"NEOKAPI_JRE_HOME": "/nonexistent"},
			existing: map[string]bool{"/install/jre/bin/java": true},
			binDir:   "/install",
			wantPath: "/install/jre/bin/java",
		},
	}

	for _, tc := range cases {
		tc := tc
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			getenv := func(k string) string { return tc.env[k] }
			stat := func(p string) (os.FileInfo, error) {
				if tc.existing[p] {
					return fakeFileInfo{}, nil
				}
				return nil, os.ErrNotExist
			}
			lookPath := func(name string) (string, error) {
				if tc.pathFound != "" {
					return tc.pathFound, nil
				}
				if tc.pathErr != nil {
					return "", tc.pathErr
				}
				return "", os.ErrNotExist
			}
			got, err := findJava(tc.binDir, getenv, stat, lookPath)
			if tc.wantErrSub != "" {
				if err == nil {
					t.Fatalf("expected error containing %q, got nil (path %q)", tc.wantErrSub, got)
				}
				if !strings.Contains(err.Error(), tc.wantErrSub) {
					t.Fatalf("error %q does not contain %q", err.Error(), tc.wantErrSub)
				}
				return
			}
			if err != nil {
				t.Fatalf("unexpected error: %v", err)
			}
			if got != tc.wantPath {
				t.Fatalf("got %q, want %q", got, tc.wantPath)
			}
		})
	}
}

func TestWriteHandshake(t *testing.T) {
	t.Parallel()
	var buf bytes.Buffer
	if err := writeHandshake(&buf, "/tmp/x.sock", "1.47.0"); err != nil {
		t.Fatalf("writeHandshake: %v", err)
	}
	if !bytes.HasSuffix(buf.Bytes(), []byte{'\n'}) {
		t.Fatalf("handshake must end with newline; got %q", buf.String())
	}
	var got handshakePayload
	if err := json.Unmarshal(bytes.TrimRight(buf.Bytes(), "\n"), &got); err != nil {
		t.Fatalf("unmarshal: %v\npayload: %s", err, buf.String())
	}
	if got.Socket != "/tmp/x.sock" {
		t.Fatalf("socket: got %q want %q", got.Socket, "/tmp/x.sock")
	}
	if got.Version != "1.47.0" {
		t.Fatalf("version: got %q want %q", got.Version, "1.47.0")
	}
}

func TestReadFirstLine(t *testing.T) {
	t.Parallel()
	cases := []struct {
		name     string
		input    string
		max      int
		wantLine string
		wantErr  bool
	}{
		{"basic", "hello\nworld\n", 100, "hello", false},
		{"only-line-no-newline-eof", "tail", 100, "tail", false},
		{"empty-eof", "", 100, "", true},
		{"exceeds-max", "abcdefghij", 4, "abcd", true},
	}
	for _, tc := range cases {
		tc := tc
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			got, err := readFirstLine(strings.NewReader(tc.input), tc.max)
			if (err != nil) != tc.wantErr {
				t.Fatalf("err=%v wantErr=%v", err, tc.wantErr)
			}
			if got != tc.wantLine {
				t.Fatalf("got %q want %q", got, tc.wantLine)
			}
		})
	}
}

func TestRunArgvRouting(t *testing.T) {
	cases := []struct {
		name       string
		args       []string
		wantErrSub string
	}{
		{"no args", nil, "missing subcommand"},
		{"unknown", []string{"frobnicate"}, "unknown subcommand"},
	}
	for _, tc := range cases {
		tc := tc
		t.Run(tc.name, func(t *testing.T) {
			err := run(tc.args)
			if tc.wantErrSub == "" {
				if err != nil {
					t.Fatalf("unexpected error: %v", err)
				}
				return
			}
			if err == nil || !strings.Contains(err.Error(), tc.wantErrSub) {
				t.Fatalf("got %v, want error containing %q", err, tc.wantErrSub)
			}
		})
	}
}

func TestRunVersion(t *testing.T) {
	// Capture stdout while calling run("version"). We use os.Pipe so we
	// don't have to refactor run() to accept an io.Writer just for tests.
	r, w, err := os.Pipe()
	if err != nil {
		t.Fatalf("pipe: %v", err)
	}
	saved := os.Stdout
	os.Stdout = w
	defer func() { os.Stdout = saved }()

	prevVersion := Version
	Version = "1.47.0-test"
	defer func() { Version = prevVersion }()

	done := make(chan error, 1)
	go func() { done <- run([]string{"version"}) }()

	if err := <-done; err != nil {
		t.Fatalf("run: %v", err)
	}
	_ = w.Close()
	out, err := io.ReadAll(r)
	if err != nil {
		t.Fatalf("read: %v", err)
	}
	if strings.TrimSpace(string(out)) != "1.47.0-test" {
		t.Fatalf("got %q want %q", string(out), "1.47.0-test")
	}
}

func TestDefaultSocketPathShape(t *testing.T) {
	customTmp := "/var/tmp/shim-test"
	t.Setenv("TMPDIR", customTmp)
	got := defaultSocketPath(12345)
	if !strings.HasSuffix(got, "/kob-12345.sock") {
		t.Fatalf("default socket path %q has unexpected suffix", got)
	}
	if !strings.HasPrefix(got, customTmp) {
		t.Fatalf("default socket path %q does not honor TMPDIR=%q", got, customTmp)
	}
}
