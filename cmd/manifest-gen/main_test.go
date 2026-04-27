package main

import (
	"encoding/json"
	"os"
	"path/filepath"
	"strings"
	"testing"
)

func TestNormalizeExtensions(t *testing.T) {
	t.Parallel()
	cases := []struct {
		in   []string
		want []string
	}{
		{nil, nil},
		{[]string{}, nil},
		{[]string{"html", ".htm"}, []string{".html", ".htm"}},
		{[]string{".idml", " .idml ", ""}, []string{".idml"}},
		{[]string{".xliff", ".xlf", ".xliff"}, []string{".xliff", ".xlf"}},
	}
	for _, tc := range cases {
		got := normalizeExtensions(tc.in)
		if !sliceEq(got, tc.want) {
			t.Errorf("normalizeExtensions(%v) = %v, want %v", tc.in, got, tc.want)
		}
	}
}

func TestPickCapabilities(t *testing.T) {
	t.Parallel()
	got := pickCapabilities(nil)
	if !sliceEq(got, []string{"read", "write"}) {
		t.Errorf("default = %v", got)
	}
	got = pickCapabilities([]string{"read"})
	if !sliceEq(got, []string{"read"}) {
		t.Errorf("explicit read-only = %v", got)
	}
	got = pickCapabilities([]string{"read", "read", "write"})
	if !sliceEq(got, []string{"read", "write"}) {
		t.Errorf("dedupe = %v", got)
	}
}

func TestExpandSchemaPath(t *testing.T) {
	t.Parallel()
	if expandSchemaPath("", "okf_html") != "" {
		t.Errorf("empty pattern should yield empty path")
	}
	got := expandSchemaPath("schemas/{id}.json", "okf_html")
	if got != "schemas/okf_html.json" {
		t.Errorf("got %q", got)
	}
}

func TestGenerate_Shape(t *testing.T) {
	t.Parallel()
	input := `{
		"filters": [
			{
				"filter_class": "net.sf.okapi.filters.html.HtmlFilter",
				"id": "okf_html",
				"name": "okf_html",
				"display_name": "HTML Filter",
				"mime_types": ["text/html"],
				"extensions": [".html", ".htm"],
				"capabilities": ["read", "write"]
			},
			{
				"filter_class": "net.sf.okapi.filters.json.JSONFilter",
				"id": "okf_json",
				"name": "okf_json",
				"display_name": "JSON Filter",
				"mime_types": ["application/json"],
				"extensions": ["json"],
				"capabilities": null
			},
			{
				"id": "okf_autoxliff",
				"name": "okf_autoxliff",
				"display_name": "Auto XLIFF",
				"extensions": [".xlf", ".xliff"]
			}
		]
	}`

	m, err := Generate([]byte(input), Options{
		Plugin:                "okapi-bridge",
		Version:               "1.47.0",
		Binary:                "kapi-okapi-bridge",
		License:               "Apache-2.0",
		Description:           "test",
		Homepage:              "https://example.test",
		Author:                "neokapi",
		Group:                 "neokapi",
		IdleTimeoutSeconds:    300,
		StartupTimeoutSeconds: 30,
		SchemaPathPattern:     "schemas/{id}.schema.json",
	})
	if err != nil {
		t.Fatalf("Generate: %v", err)
	}

	if m.ManifestVersion != "1" {
		t.Errorf("manifest_version = %q", m.ManifestVersion)
	}
	if m.Plugin != "okapi-bridge" || m.Version != "1.47.0" || m.Binary != "kapi-okapi-bridge" {
		t.Errorf("identity wrong: %+v", m)
	}
	if m.Daemon == nil || m.Daemon.Handshake == nil {
		t.Fatalf("daemon block missing")
	}
	if m.Daemon.Handshake.Type != "stdio-handshake" {
		t.Errorf("handshake type %q", m.Daemon.Handshake.Type)
	}
	if !sliceEq(m.Daemon.Handshake.Fields, []string{"socket", "version"}) {
		t.Errorf("handshake fields %v", m.Daemon.Handshake.Fields)
	}
	if m.Daemon.IdleTimeoutSeconds != 300 || m.Daemon.StartupTimeoutSeconds != 30 {
		t.Errorf("daemon timeouts wrong: %+v", m.Daemon)
	}

	// okf_autoxliff must be filtered out by default.
	if got := len(m.Capabilities.Formats); got != 2 {
		t.Fatalf("expected 2 formats (autoxliff filtered), got %d: %+v", got, m.Capabilities.Formats)
	}

	// Stable ordering: alphabetical by Name.
	if m.Capabilities.Formats[0].Name != "okf_html" || m.Capabilities.Formats[1].Name != "okf_json" {
		t.Errorf("ordering wrong: %v / %v", m.Capabilities.Formats[0].Name, m.Capabilities.Formats[1].Name)
	}

	html := m.Capabilities.Formats[0]
	if html.DisplayName != "HTML Filter" {
		t.Errorf("display name = %q", html.DisplayName)
	}
	if !sliceEq(html.Extensions, []string{".html", ".htm"}) {
		t.Errorf("extensions = %v", html.Extensions)
	}
	if !sliceEq(html.MimeTypes, []string{"text/html"}) {
		t.Errorf("mime types = %v", html.MimeTypes)
	}
	if !sliceEq(html.Capabilities, []string{"read", "write"}) {
		t.Errorf("capabilities = %v", html.Capabilities)
	}
	if html.Schema != "schemas/okf_html.schema.json" {
		t.Errorf("schema path = %q", html.Schema)
	}

	// json filter: had nil capabilities → defaults to read+write; had bare
	// "json" extension → normalized to ".json".
	js := m.Capabilities.Formats[1]
	if !sliceEq(js.Capabilities, []string{"read", "write"}) {
		t.Errorf("json capabilities default = %v", js.Capabilities)
	}
	if !sliceEq(js.Extensions, []string{".json"}) {
		t.Errorf("json extensions normalized = %v", js.Extensions)
	}
}

func TestGenerate_RequiredFields(t *testing.T) {
	t.Parallel()
	cases := []struct {
		name    string
		opts    Options
		wantSub string
	}{
		{"no plugin", Options{Version: "1", Binary: "x"}, "plugin name"},
		{"no version", Options{Plugin: "p", Binary: "x"}, "plugin version"},
		{"no binary", Options{Plugin: "p", Version: "1"}, "plugin binary"},
	}
	for _, tc := range cases {
		_, err := Generate([]byte(`{"filters":[]}`), tc.opts)
		if err == nil || !strings.Contains(err.Error(), tc.wantSub) {
			t.Errorf("%s: got %v, want substring %q", tc.name, err, tc.wantSub)
		}
	}
}

func TestGenerate_AllowAutoXLIFF(t *testing.T) {
	t.Parallel()
	input := `{"filters":[{"id":"okf_autoxliff","display_name":"Auto"}]}`
	m, err := Generate([]byte(input), Options{
		Plugin: "p", Version: "1", Binary: "b",
		IncludeAutoXLIFF: true,
	})
	if err != nil {
		t.Fatalf("Generate: %v", err)
	}
	if len(m.Capabilities.Formats) != 1 {
		t.Fatalf("expected 1 format, got %d", len(m.Capabilities.Formats))
	}
}

func TestGenerate_BadJSON(t *testing.T) {
	t.Parallel()
	_, err := Generate([]byte("not json"), Options{Plugin: "p", Version: "1", Binary: "b"})
	if err == nil {
		t.Fatalf("expected parse error")
	}
}

func TestWriteJSON_Roundtrip(t *testing.T) {
	t.Parallel()
	dir := t.TempDir()
	path := filepath.Join(dir, "manifest.json")

	m := &Manifest{
		ManifestVersion: "1",
		Plugin:          "okapi-bridge",
		Version:         "1.47.0",
		Binary:          "kapi-okapi-bridge",
		License:         "Apache-2.0",
		Capabilities: Capabilities{
			Formats: []Format{{Name: "okf_html", Capabilities: []string{"read"}}},
		},
		Daemon: &DaemonConfig{
			IdleTimeoutSeconds:    300,
			StartupTimeoutSeconds: 30,
			Handshake: &Handshake{
				Type:   "stdio-handshake",
				Fields: []string{"socket", "version"},
			},
		},
	}
	if err := writeJSON(path, m); err != nil {
		t.Fatalf("writeJSON: %v", err)
	}
	raw, err := os.ReadFile(path)
	if err != nil {
		t.Fatalf("read: %v", err)
	}
	// Indented JSON should contain at least one newline.
	if !strings.Contains(string(raw), "\n") {
		t.Errorf("output not indented")
	}

	var got Manifest
	if err := json.Unmarshal(raw, &got); err != nil {
		t.Fatalf("unmarshal: %v\n%s", err, raw)
	}
	if got.Plugin != "okapi-bridge" || got.Daemon == nil || got.Daemon.Handshake.Type != "stdio-handshake" {
		t.Errorf("roundtrip mismatch: %+v", got)
	}
}

func sliceEq(a, b []string) bool {
	if len(a) != len(b) {
		return false
	}
	for i := range a {
		if a[i] != b[i] {
			return false
		}
	}
	return true
}
