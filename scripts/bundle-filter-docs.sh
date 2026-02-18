#!/bin/bash
# Bundle parsed filter documentation into a single JSON file for UI consumption
# Usage: ./scripts/bundle-filter-docs.sh [filter-docs-dir]
#
# Reads individual parsed JSON files and produces filter-docs-bundle.json

set -e

DOCS_DIR="${1:-filter-docs}"
PARSED_DIR="$DOCS_DIR/parsed"
OUTPUT_FILE="$DOCS_DIR/filter-docs-bundle.json"

if [ ! -d "$PARSED_DIR" ]; then
    echo "Error: $PARSED_DIR not found. Run 'make parse-filter-docs' first."
    exit 1
fi

echo "Bundling filter documentation..."

# Build a JSON object: { filters: { filterId: docData, ... }, generatedAt: ... }
# Skip symlinks (secondary filter IDs) and index.json
jq -n --arg date "$(date -u +%Y-%m-%dT%H:%M:%SZ)" '
{
  generatedAt: $date,
  filters: {}
}' > "$OUTPUT_FILE.tmp"

filter_count=0
for json_file in "$PARSED_DIR"/okf_*.json; do
    # Skip symlinks — they'll be handled as aliases
    [ -L "$json_file" ] && continue
    [ ! -f "$json_file" ] && continue
    
    filter_id=$(basename "$json_file" .json)
    
    # Add this filter's doc data to the bundle
    jq --arg id "$filter_id" --slurpfile doc "$json_file" \
        '.filters[$id] = $doc[0]' "$OUTPUT_FILE.tmp" > "$OUTPUT_FILE.tmp2"
    mv "$OUTPUT_FILE.tmp2" "$OUTPUT_FILE.tmp"
    ((filter_count++))
done

# Add symlink aliases (secondary filter IDs → primary filter ID)
for json_file in "$PARSED_DIR"/okf_*.json; do
    [ ! -L "$json_file" ] && continue
    alias_id=$(basename "$json_file" .json)
    target=$(readlink "$json_file")
    primary_id=$(basename "$target" .json)
    
    jq --arg alias "$alias_id" --arg primary "$primary_id" \
        '.aliases[$alias] = $primary' "$OUTPUT_FILE.tmp" > "$OUTPUT_FILE.tmp2"
    mv "$OUTPUT_FILE.tmp2" "$OUTPUT_FILE.tmp"
done

mv "$OUTPUT_FILE.tmp" "$OUTPUT_FILE"

size_kb=$(( $(wc -c < "$OUTPUT_FILE") / 1024 ))
echo "✓ Bundled $filter_count filters into $OUTPUT_FILE (${size_kb} KB)"
