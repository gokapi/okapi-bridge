#!/usr/bin/env bash
# Package a v2 manifest-driven plugin tarball for one (okapi-version, os, arch).
#
# Layout produced:
#
#   kapi-okapi-bridge_<okapi-version>_<os>_<arch>.tar.gz
#   └── okapi-bridge/
#       ├── manifest.json
#       ├── kapi-okapi-bridge        (Go shim, statically linked)
#       ├── jars/
#       │   └── neokapi-bridge-jar-with-dependencies.jar
#       └── jre/                      (optional — bundled Temurin JRE)
#           └── bin/java
#
# Inputs are passed via flags. The script does NOT build the JAR — it
# expects okapi-releases/<okapi-version>/target/neokapi-bridge-*.jar
# already exists (Maven step in the workflow runs first).
#
# Usage:
#   scripts/package-release-v2.sh \
#     --okapi-version 1.47.0 \
#     --java-version 11 \
#     --os linux --arch amd64 \
#     --plugin okapi-bridge \
#     [--bundle-jre] \
#     [--no-tarball]
#
# Outputs:
#   dist/v2/<plugin-name>-<okapi-version>-<os>-<arch>/  (staging dir)
#   dist/v2/kapi-okapi-bridge_<okapi-version>_<os>_<arch>.tar.gz
#
# Maps Go GOOS/GOARCH → Adoptium os/arch:
#   GOOS    Adoptium os
#   linux   linux
#   darwin  mac
#   windows windows
#   GOARCH  Adoptium arch
#   amd64   x64
#   arm64   aarch64
set -euo pipefail

OKAPI_VERSION=""
JAVA_VERSION=""
OS=""
ARCH=""
PLUGIN_NAME="okapi-bridge"
BINARY_NAME="kapi-okapi-bridge"
BUNDLE_JRE=0
TARBALL=1

while [[ $# -gt 0 ]]; do
    case "$1" in
        --okapi-version) OKAPI_VERSION="$2"; shift 2 ;;
        --java-version) JAVA_VERSION="$2"; shift 2 ;;
        --os) OS="$2"; shift 2 ;;
        --arch) ARCH="$2"; shift 2 ;;
        --plugin) PLUGIN_NAME="$2"; shift 2 ;;
        --binary) BINARY_NAME="$2"; shift 2 ;;
        --bundle-jre) BUNDLE_JRE=1; shift ;;
        --no-tarball) TARBALL=0; shift ;;
        *) echo "unknown flag: $1" >&2; exit 2 ;;
    esac
done

for required_flag in OKAPI_VERSION OS ARCH; do
    if [[ -z "${!required_flag}" ]]; then
        echo "missing required flag: --${required_flag,,}" >&2
        exit 2
    fi
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$ROOT_DIR"

# Look up java-version from meta.json if not supplied.
if [[ -z "$JAVA_VERSION" ]]; then
    META="okapi-releases/${OKAPI_VERSION}/meta.json"
    if [[ ! -f "$META" ]]; then
        echo "package-release-v2: --java-version not supplied and $META missing" >&2
        exit 1
    fi
    JAVA_VERSION=$(jq -r '.javaVersion' "$META")
    if [[ "$JAVA_VERSION" == "null" || -z "$JAVA_VERSION" ]]; then
        echo "package-release-v2: javaVersion missing from $META" >&2
        exit 1
    fi
fi

# Resolve GOOS/GOARCH → Adoptium taxonomy.
case "$OS" in
    linux)   ADOPTIUM_OS="linux" ;;
    darwin)  ADOPTIUM_OS="mac" ;;
    windows) ADOPTIUM_OS="windows" ;;
    *) echo "unsupported --os $OS" >&2; exit 2 ;;
esac
case "$ARCH" in
    amd64) ADOPTIUM_ARCH="x64" ;;
    arm64) ADOPTIUM_ARCH="aarch64" ;;
    *) echo "unsupported --arch $ARCH" >&2; exit 2 ;;
esac

JAR_DIR="okapi-releases/${OKAPI_VERSION}/target"
JAR=$(ls "$JAR_DIR"/neokapi-bridge-*-jar-with-dependencies.jar 2>/dev/null | grep -v original- | head -n1 || true)
if [[ -z "$JAR" ]]; then
    echo "package-release-v2: no jar-with-dependencies in $JAR_DIR; build with 'make build V=$OKAPI_VERSION' first" >&2
    exit 1
fi

STAGE_DIR="dist/v2/${PLUGIN_NAME}-${OKAPI_VERSION}-${OS}-${ARCH}/${PLUGIN_NAME}"
rm -rf "$STAGE_DIR"
mkdir -p "$STAGE_DIR/jars"

# 1. Build the shim.
SHIM_OUT="$STAGE_DIR/$BINARY_NAME"
"$SCRIPT_DIR/build-shim.sh" \
    --os "$OS" --arch "$ARCH" \
    --okapi-version "$OKAPI_VERSION" \
    --output "$SHIM_OUT"
chmod +x "$SHIM_OUT"

# 2. Stage the JAR.
cp "$JAR" "$STAGE_DIR/jars/neokapi-bridge-jar-with-dependencies.jar"

# 3. Generate the manifest from the live filter introspection.
#    OkapiBridgeServer --list-filters runs against the host JRE, not the
#    bundled one, so we just need any java on PATH that can load this jar.
if ! command -v java >/dev/null 2>&1; then
    echo "package-release-v2: java not on PATH; needed to introspect filters" >&2
    exit 1
fi

FILTERS_JSON="$(mktemp)"
trap 'rm -f "$FILTERS_JSON"' EXIT

java -cp "$JAR" neokapi.bridge.OkapiBridgeServer --list-filters > "$FILTERS_JSON"

go run ./cmd/manifest-gen \
    --okapi-version "$OKAPI_VERSION" \
    --filters-json "$FILTERS_JSON" \
    --plugin "$PLUGIN_NAME" \
    --binary "$BINARY_NAME" \
    --output "$STAGE_DIR/manifest.json"

# 4. Optionally bundle the JRE.
if [[ "$BUNDLE_JRE" == "1" ]]; then
    "$SCRIPT_DIR/bundle-jre.sh" \
        --java-version "$JAVA_VERSION" \
        --os "$ADOPTIUM_OS" \
        --arch "$ADOPTIUM_ARCH" \
        --output "$STAGE_DIR/jre"
fi

echo "package-release-v2: staged $STAGE_DIR" >&2

# 5. Tarball.
if [[ "$TARBALL" == "1" ]]; then
    TARBALL_NAME="${BINARY_NAME}_${OKAPI_VERSION}_${OS}_${ARCH}.tar.gz"
    TARBALL_PATH="dist/v2/$TARBALL_NAME"
    rm -f "$TARBALL_PATH"
    # Make the tar reproducible-ish: sorted, no owner/group metadata.
    tar -C "$(dirname "$STAGE_DIR")" \
        --no-xattrs \
        --owner=0 --group=0 \
        -czf "$TARBALL_PATH" \
        "$PLUGIN_NAME" 2>/dev/null \
        || tar -C "$(dirname "$STAGE_DIR")" -czf "$TARBALL_PATH" "$PLUGIN_NAME"

    sha256sum "$TARBALL_PATH" > "$TARBALL_PATH.sha256" 2>/dev/null \
        || shasum -a 256 "$TARBALL_PATH" > "$TARBALL_PATH.sha256"

    echo "package-release-v2: wrote $TARBALL_PATH" >&2
    echo "$TARBALL_PATH"
fi
