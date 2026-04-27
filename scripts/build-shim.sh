#!/usr/bin/env bash
# Cross-compile the kapi-okapi-bridge Go shim for one (os, arch) combo.
#
# Usage:
#   scripts/build-shim.sh \
#     --os darwin --arch arm64 \
#     --okapi-version 1.47.0 \
#     --output ./stage/kapi-okapi-bridge
#
# The --okapi-version is embedded into the shim binary via -ldflags so
# `kapi-okapi-bridge version` and the v2 handshake JSON line both report
# the bundled Okapi Framework version.
set -euo pipefail

OS=""
ARCH=""
OKAPI_VERSION=""
OUTPUT=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --os) OS="$2"; shift 2 ;;
        --arch) ARCH="$2"; shift 2 ;;
        --okapi-version) OKAPI_VERSION="$2"; shift 2 ;;
        --output) OUTPUT="$2"; shift 2 ;;
        *) echo "unknown flag: $1" >&2; exit 2 ;;
    esac
done

if [[ -z "$OS" || -z "$ARCH" || -z "$OKAPI_VERSION" || -z "$OUTPUT" ]]; then
    echo "usage: $0 --os <linux|darwin|windows> --arch <amd64|arm64> --okapi-version <version> --output <path>" >&2
    exit 2
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$ROOT_DIR"

mkdir -p "$(dirname "$OUTPUT")"

echo "build-shim: GOOS=$OS GOARCH=$ARCH version=$OKAPI_VERSION → $OUTPUT" >&2
GOOS="$OS" GOARCH="$ARCH" CGO_ENABLED=0 \
    go build \
        -ldflags "-s -w -X main.Version=${OKAPI_VERSION}" \
        -o "$OUTPUT" \
        ./cmd/shim

echo "build-shim: done $(file "$OUTPUT" | head -1)" >&2
