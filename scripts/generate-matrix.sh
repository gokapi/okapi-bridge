#!/bin/bash
# Generate markdown matrix of Okapi versions vs schema versions
# Usage: generate-matrix.sh [--compact]

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$ROOT_DIR"

VERSIONS_FILE="schema-versions.json"

# Get Okapi versions from okapi-releases directories
get_okapi_versions() {
    ls -1 okapi-releases 2>/dev/null | sort -V
}

# Get filters with multiple versions (interesting ones)
get_versioned_filters() {
    jq -r '.filters | to_entries[] | select(.value.versions | length > 1) | .key' "$VERSIONS_FILE" | sort
}

# Get all filters
get_all_filters() {
    jq -r '.filters | keys[]' "$VERSIONS_FILE" | sort
}

# Get schema version for a filter at an Okapi version
get_schema_version() {
    local filter="$1"
    local okapi_version="$2"
    jq -r --arg f "$filter" --arg ov "$okapi_version" '
        .filters[$f].versions[] | 
        select(.okapiVersions | index($ov)) | 
        .version // empty
    ' "$VERSIONS_FILE" | head -1
}

# Generate compact matrix (only filters with changes)
generate_compact_matrix() {
    echo "## Schema Version Matrix"
    echo ""
    echo "Shows which composite schema version applies to each Okapi release."
    echo "Only filters with version changes across releases are shown."
    echo ""
    
    local okapi_versions
    okapi_versions=$(get_okapi_versions)
    
    local filters
    filters=$(get_versioned_filters)
    
    # Header row
    printf "| Filter |"
    for v in $okapi_versions; do
        printf " %s |" "$v"
    done
    echo ""
    
    # Separator
    printf "%s" "|--------|"
    for v in $okapi_versions; do
        printf "%s" "------|"
    done
    echo ""
    
    # Data rows
    for filter in $filters; do
        printf "| \`%s\` |" "$filter"
        local prev_ver=""
        for okapi in $okapi_versions; do
            local ver
            ver=$(get_schema_version "$filter" "$okapi")
            if [[ -n "$ver" ]]; then
                if [[ "$ver" == "$prev_ver" ]]; then
                    printf " → |"
                else
                    printf " v%s |" "$ver"
                fi
                prev_ver="$ver"
            else
                printf " - |"
                prev_ver=""
            fi
        done
        echo ""
    done
}

# Generate full matrix
generate_full_matrix() {
    echo "## Schema Version Matrix (Full)"
    echo ""
    
    local okapi_versions
    okapi_versions=$(get_okapi_versions)
    
    local filters
    filters=$(get_all_filters)
    
    # Header
    printf "| Filter |"
    for v in $okapi_versions; do
        printf " %s |" "$v"
    done
    echo ""
    
    # Separator
    printf "%s" "|--------|"
    for v in $okapi_versions; do
        printf "%s" "------|"
    done
    echo ""
    
    # Data
    for filter in $filters; do
        printf "| \`%s\` |" "$filter"
        for okapi in $okapi_versions; do
            local ver
            ver=$(get_schema_version "$filter" "$okapi")
            if [[ -n "$ver" ]]; then
                printf " v%s |" "$ver"
            else
                printf " - |"
            fi
        done
        echo ""
    done
}

# Summary stats
generate_summary() {
    echo "## Schema Statistics"
    echo ""
    local total_filters=$(jq '.filters | length' "$VERSIONS_FILE")
    local total_versions=$(jq '[.filters[].versions | length] | add' "$VERSIONS_FILE")
    local filters_with_changes=$(jq '[.filters | to_entries[] | select(.value.versions | length > 1)] | length' "$VERSIONS_FILE")
    
    echo "- **Total filters**: $total_filters"
    echo "- **Total schema versions**: $total_versions"
    echo "- **Filters with version changes**: $filters_with_changes"
    echo ""
}

case "${1:-}" in
    --full)
        generate_summary
        generate_full_matrix
        ;;
    *)
        generate_summary
        generate_compact_matrix
        ;;
esac
