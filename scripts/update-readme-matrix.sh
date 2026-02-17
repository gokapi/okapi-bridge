#!/bin/bash
# Update the schema version matrix in README.md
# Replaces content between <!-- SCHEMA_MATRIX_START --> and <!-- SCHEMA_MATRIX_END -->

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$ROOT_DIR"

README="README.md"
START_MARKER="<!-- SCHEMA_MATRIX_START -->"
END_MARKER="<!-- SCHEMA_MATRIX_END -->"
TMP_FILE=$(mktemp)
CONTENT_FILE=$(mktemp)

# Generate the matrix content to a file
generate_matrix_content() {
    local total_filters=$(jq '.filters | length' schema-versions.json)
    local total_versions=$(jq '[.filters[].versions | length] | add' schema-versions.json)
    local filters_with_changes=$(jq '[.filters | to_entries[] | select(.value.versions | length > 1)] | length' schema-versions.json)
    
    cat << HEADER
### Schema Statistics

- **Total filters**: $total_filters
- **Total schema versions**: $total_versions
- **Filters with version changes**: $filters_with_changes

### Schema Version Matrix

Shows which composite schema version applies to each Okapi release.
Only filters with multiple versions are shown (\`-\` = filter not available).

HEADER

    # Generate table
    "$SCRIPT_DIR/generate-matrix.sh" 2>/dev/null | grep -E '^\|' || true
}

# Check if markers exist
if ! grep -q "$START_MARKER" "$README"; then
    echo "Error: Start marker not found in README.md"
    exit 1
fi

if ! grep -q "$END_MARKER" "$README"; then
    echo "Error: End marker not found in README.md"
    exit 1
fi

# Generate content to temp file
generate_matrix_content > "$CONTENT_FILE"

# Build new README
{
    # Everything before and including start marker
    sed -n "1,/$START_MARKER/p" "$README"
    
    # New content
    cat "$CONTENT_FILE"
    
    # Everything from end marker onwards
    sed -n "/$END_MARKER/,\$p" "$README"
} > "$TMP_FILE"

mv "$TMP_FILE" "$README"
rm -f "$CONTENT_FILE"

echo "Updated README.md with current schema matrix"
