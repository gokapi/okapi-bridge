#!/bin/bash
# Copies the correct composite schemas for a given Okapi version into a target
# directory (with unversioned names) so they can be bundled into the shaded JAR.
#
# Usage: prepare-schemas-for-jar.sh <okapi-version> <output-dir>
# Example: prepare-schemas-for-jar.sh 1.47.0 target/schema-resources/schemas

set -euo pipefail

OKAPI_VERSION="${1:?Usage: prepare-schemas-for-jar.sh <okapi-version> <output-dir>}"
OUTPUT_DIR="${2:?Usage: prepare-schemas-for-jar.sh <okapi-version> <output-dir>}"

# Find the project root
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
VERSIONS_FILE="$PROJECT_ROOT/schemas/versions.json"
COMPOSITE_DIR="$PROJECT_ROOT/schemas/composite"

if [ ! -f "$VERSIONS_FILE" ]; then
    echo "[prepare-schemas] Warning: $VERSIONS_FILE not found, skipping schema bundling" >&2
    exit 0
fi

mkdir -p "$OUTPUT_DIR"

# Use jq to find the correct schema version for each filter for this Okapi version
COPIED=0
while IFS=' ' read -r filter version; do
    src="$COMPOSITE_DIR/${filter}.v${version}.schema.json"
    dst="$OUTPUT_DIR/${filter}.schema.json"
    if [ -f "$src" ]; then
        cp "$src" "$dst"
        COPIED=$((COPIED + 1))
    else
        echo "[prepare-schemas] Warning: missing $src" >&2
    fi
done < <(jq -r --arg ov "$OKAPI_VERSION" '
    .filters | to_entries[] |
    .key as $f |
    [.value.versions[] | select(.okapiVersions | index($ov))] |
    max_by(.version) | select(.) |
    "\($f) \(.version)"
' "$VERSIONS_FILE")

echo "[prepare-schemas] Copied $COPIED schemas for Okapi $OKAPI_VERSION to $OUTPUT_DIR" >&2
