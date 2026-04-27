#!/usr/bin/env bash
# Download and stage a Temurin JRE for one (java-version, os, arch) combo.
#
# This is a shim helper used by package-release-v2.sh and CI. The JRE is
# fetched from api.adoptium.net via the v3 "binary" endpoint, which 302s us
# to the actual download URL.
#
# Usage:
#   scripts/bundle-jre.sh \
#     --java-version 11 \
#     --os linux \
#     --arch x64 \
#     --output ./stage/jre
#
# Inputs:
#   --java-version   11 | 17 (the only versions used by the bridge)
#   --os             linux | mac | windows
#   --arch           x64 | aarch64
#   --output         destination directory (will contain bin/java after extraction)
#   --image-type     jre (default) | jdk
#
# On success the output directory contains the extracted JRE root with
# bin/java inside (no extra wrapper directory).
#
# Idempotent: if --output already contains bin/java, the script is a no-op.
# Safe for repeated CI runs.
set -euo pipefail

JAVA_VERSION=""
OS=""
ARCH=""
OUTPUT=""
IMAGE_TYPE="jre"

while [[ $# -gt 0 ]]; do
    case "$1" in
        --java-version) JAVA_VERSION="$2"; shift 2 ;;
        --os) OS="$2"; shift 2 ;;
        --arch) ARCH="$2"; shift 2 ;;
        --output) OUTPUT="$2"; shift 2 ;;
        --image-type) IMAGE_TYPE="$2"; shift 2 ;;
        *) echo "unknown flag: $1" >&2; exit 2 ;;
    esac
done

if [[ -z "$JAVA_VERSION" || -z "$OS" || -z "$ARCH" || -z "$OUTPUT" ]]; then
    echo "usage: $0 --java-version <11|17> --os <linux|mac|windows> --arch <x64|aarch64> --output <dir>" >&2
    exit 2
fi

# Idempotency check.
if [[ -x "$OUTPUT/bin/java" || -x "$OUTPUT/bin/java.exe" ]]; then
    echo "bundle-jre: $OUTPUT already has bin/java; skipping download" >&2
    exit 0
fi

mkdir -p "$OUTPUT"
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

API_URL="https://api.adoptium.net/v3/binary/latest/${JAVA_VERSION}/ga/${OS}/${ARCH}/${IMAGE_TYPE}/hotspot/normal/eclipse"
echo "bundle-jre: GET $API_URL" >&2

# Adoptium 302s to the binary; -L follows.
ARCHIVE="$TMP/archive"
case "$OS" in
    windows) ARCHIVE="$ARCHIVE.zip" ;;
    *)       ARCHIVE="$ARCHIVE.tar.gz" ;;
esac

curl -fL --retry 3 --retry-delay 5 --connect-timeout 30 -o "$ARCHIVE" "$API_URL"

echo "bundle-jre: extracting $(basename "$ARCHIVE") into $OUTPUT" >&2
case "$OS" in
    windows)
        unzip -q "$ARCHIVE" -d "$TMP/extract"
        ;;
    *)
        tar -xzf "$ARCHIVE" -C "$TMP/extract" || {
            mkdir -p "$TMP/extract"
            tar -xzf "$ARCHIVE" -C "$TMP/extract"
        }
        ;;
esac

# Adoptium archives wrap everything in a single jdk-* / jre-* directory.
inner=$(find "$TMP/extract" -mindepth 1 -maxdepth 1 -type d | head -n1)
if [[ -z "$inner" ]]; then
    echo "bundle-jre: archive layout unexpected, no inner directory" >&2
    exit 1
fi

# macOS Adoptium archives have a Contents/Home wrapper; flatten to the JRE root.
if [[ -d "$inner/Contents/Home" ]]; then
    inner="$inner/Contents/Home"
fi

# Copy into output.
cp -R "$inner"/* "$OUTPUT"/

if [[ ! -x "$OUTPUT/bin/java" && ! -x "$OUTPUT/bin/java.exe" ]]; then
    echo "bundle-jre: extraction did not produce bin/java in $OUTPUT" >&2
    exit 1
fi

echo "bundle-jre: ready: $OUTPUT/bin/java" >&2
