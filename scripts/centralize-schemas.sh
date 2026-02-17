#!/bin/bash
# Centralize schemas from per-version directories
# Usage: centralize-schemas.sh [regenerate-composites]
#
# Operations:
#   (default)           - Full regeneration: generate bases and composites for all versions
#   regenerate-composites - Only regenerate composites from existing bases + overrides

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$ROOT_DIR"

SCHEMAS_DIR="schemas"
BASE_DIR="$SCHEMAS_DIR/base"
COMPOSITE_DIR="$SCHEMAS_DIR/composite"
OVERRIDES_DIR="overrides"
VERSIONS_FILE="schema-versions.json"

# Get all Okapi versions sorted
get_versions() {
    ls -1 okapi-releases 2>/dev/null | sort -V
}

# Initialize schema-versions.json if needed
init_versions_file() {
    if [[ ! -f "$VERSIONS_FILE" ]]; then
        echo '{"$schema":"https://gokapi.dev/schemas/schema-versions.json","generatedAt":"","filters":{}}' > "$VERSIONS_FILE"
    fi
}

# Update generatedAt timestamp
update_timestamp() {
    local timestamp
    timestamp=$(date -u +%Y-%m-%dT%H:%M:%SZ)
    jq --arg ts "$timestamp" '.generatedAt = $ts' "$VERSIONS_FILE" > "$VERSIONS_FILE.tmp" && mv "$VERSIONS_FILE.tmp" "$VERSIONS_FILE"
}

# Get composite version for a filter/hash combination
get_composite_version() {
    local filter="$1"
    local composite_hash="$2"
    jq -r --arg f "$filter" --arg h "$composite_hash" '
        .filters[$f].versions[]? | select(.compositeHash == $h) | .version // empty
    ' "$VERSIONS_FILE"
}

# Get next version number for a filter
get_next_version() {
    local filter="$1"
    jq -r --arg f "$filter" '
        (.filters[$f].versions // []) | map(.version) | max // 0 | . + 1
    ' "$VERSIONS_FILE"
}

# Add or update version entry in schema-versions.json
update_version_entry() {
    local filter="$1"
    local version="$2"
    local base_hash="$3"
    local override_hash="$4"
    local composite_hash="$5"
    local okapi_version="$6"
    local introduced_in="$7"
    
    # Check if this version already exists
    local existing
    existing=$(jq -r --arg f "$filter" --arg v "$version" '
        .filters[$f].versions[]? | select(.version == ($v | tonumber)) | .version // empty
    ' "$VERSIONS_FILE")
    
    if [[ -n "$existing" ]]; then
        # Add okapi version to existing entry
        jq --arg f "$filter" --arg v "$version" --arg ov "$okapi_version" '
            .filters[$f].versions |= map(
                if .version == ($v | tonumber) then
                    .okapiVersions |= (. + [$ov] | unique)
                else .
                end
            )
        ' "$VERSIONS_FILE" > "$VERSIONS_FILE.tmp" && mv "$VERSIONS_FILE.tmp" "$VERSIONS_FILE"
    else
        # Create new version entry
        local override_json="null"
        if [[ -n "$override_hash" ]]; then
            override_json="\"$override_hash\""
        fi
        
        jq --arg f "$filter" \
           --argjson v "$version" \
           --arg bh "$base_hash" \
           --argjson oh "$override_json" \
           --arg ch "$composite_hash" \
           --arg ov "$okapi_version" \
           --arg intro "$introduced_in" '
            .filters[$f].versions //= [] |
            .filters[$f].versions += [{
                version: $v,
                baseHash: $bh,
                overrideHash: $oh,
                compositeHash: $ch,
                introducedInOkapi: $intro,
                okapiVersions: [$ov]
            }]
        ' "$VERSIONS_FILE" > "$VERSIONS_FILE.tmp" && mv "$VERSIONS_FILE.tmp" "$VERSIONS_FILE"
    fi
}

# Process a single base schema for an Okapi version
process_schema() {
    local okapi_version="$1"
    local base_file="$2"
    
    local filename
    filename=$(basename "$base_file")
    local filter="${filename%.schema.json}"
    
    # Compute base hash
    local base_hash
    base_hash=$("$SCRIPT_DIR/compute-hash.sh" "$base_file")
    
    # Check for override
    local override_file="$OVERRIDES_DIR/${filter}.overrides.json"
    local override_hash=""
    if [[ -f "$override_file" ]]; then
        override_hash=$("$SCRIPT_DIR/compute-hash.sh" "$override_file")
    fi
    
    # Generate composite
    local tmp_composite="/tmp/${filter}.composite.json"
    "$SCRIPT_DIR/merge-schema.sh" "$base_file" "$override_file" "$tmp_composite" 2>/dev/null || \
        cp "$base_file" "$tmp_composite"
    
    # Compute composite hash
    local composite_hash
    composite_hash=$("$SCRIPT_DIR/compute-hash.sh" "$tmp_composite")
    
    # Check if this composite already exists
    local existing_version
    existing_version=$(get_composite_version "$filter" "$composite_hash")
    
    if [[ -n "$existing_version" ]]; then
        # Same composite - just add Okapi version to existing
        update_version_entry "$filter" "$existing_version" "$base_hash" "$override_hash" "$composite_hash" "$okapi_version" ""
        echo "  = $filter v$existing_version (unchanged)"
        rm -f "$tmp_composite"
        return
    fi
    
    # New composite version needed
    local new_version
    new_version=$(get_next_version "$filter")
    
    # Save base schema (deduplicated by hash)
    local base_output="$BASE_DIR/${filter}.base.${base_hash}.json"
    if [[ ! -f "$base_output" ]]; then
        cp "$base_file" "$base_output"
    fi
    
    # Save composite schema
    local composite_output="$COMPOSITE_DIR/${filter}.v${new_version}.schema.json"
    
    # Add version metadata to composite
    jq --argjson v "$new_version" \
       --arg intro "$okapi_version" \
       --arg bh "$base_hash" \
       --arg ch "$composite_hash" '
        . + {
            "$version": "\($v).0.0",
            "x-schemaVersion": $v,
            "x-introducedInOkapi": $intro,
            "x-baseHash": $bh,
            "x-compositeHash": $ch
        }
    ' "$tmp_composite" > "$composite_output"
    
    rm -f "$tmp_composite"
    
    # Update versions file
    update_version_entry "$filter" "$new_version" "$base_hash" "$override_hash" "$composite_hash" "$okapi_version" "$okapi_version"
    
    if [[ "$new_version" -eq 1 ]]; then
        echo "  + $filter v1 (new)"
    else
        echo "  ↑ $filter v$new_version (changed)"
    fi
}

# Main: regenerate all schemas
regenerate_all() {
    echo "Centralizing schemas..."
    
    # Create directories
    mkdir -p "$BASE_DIR" "$COMPOSITE_DIR"
    
    # Initialize versions file
    init_versions_file
    
    # Process each Okapi version
    for version in $(get_versions); do
        local schemas_dir="okapi-releases/$version/schemas"
        
        if [[ ! -d "$schemas_dir" ]]; then
            echo "=== Okapi $version ==="
            echo "  Generating base schemas..."
            
            # Generate schemas using Java
            mkdir -p "$schemas_dir"
            if [[ -f "okapi-releases/$version/pom.xml" ]]; then
                mvn -B -q compile -f "okapi-releases/$version/pom.xml" 2>/dev/null || true
                mvn -B -q exec:java@generate-schemas -Dexec.args="$schemas_dir" -f "okapi-releases/$version/pom.xml" 2>/dev/null || true
            fi
        fi
        
        if [[ ! -d "$schemas_dir" ]] || [[ -z "$(ls -A "$schemas_dir"/*.schema.json 2>/dev/null)" ]]; then
            echo "=== Okapi $version ==="
            echo "  No schemas found, skipping"
            continue
        fi
        
        echo "=== Okapi $version ==="
        
        for schema_file in "$schemas_dir"/*.schema.json; do
            [[ -f "$schema_file" ]] || continue
            [[ "$(basename "$schema_file")" == "meta.json" ]] && continue
            process_schema "$version" "$schema_file"
        done
    done
    
    # Update timestamp
    update_timestamp
    
    echo ""
    echo "=== Summary ==="
    echo "Base schemas: $(ls -1 "$BASE_DIR" 2>/dev/null | wc -l | tr -d ' ')"
    echo "Composite schemas: $(ls -1 "$COMPOSITE_DIR" 2>/dev/null | wc -l | tr -d ' ')"
    echo "Filters: $(jq '.filters | length' "$VERSIONS_FILE")"
}

# Regenerate composites only (when overrides change)
regenerate_composites() {
    echo "Regenerating composite schemas..."
    
    if [[ ! -d "$BASE_DIR" ]]; then
        echo "Error: No base schemas found. Run full regeneration first." >&2
        exit 1
    fi
    
    # Clear composites
    rm -f "$COMPOSITE_DIR"/*.schema.json
    
    # Reset versions file (keep structure, clear versions)
    jq '.filters = {}' "$VERSIONS_FILE" > "$VERSIONS_FILE.tmp" && mv "$VERSIONS_FILE.tmp" "$VERSIONS_FILE"
    
    # Process each Okapi version
    for version in $(get_versions); do
        local schemas_dir="okapi-releases/$version/schemas"
        
        if [[ ! -d "$schemas_dir" ]] || [[ -z "$(ls -A "$schemas_dir"/*.schema.json 2>/dev/null)" ]]; then
            continue
        fi
        
        echo "=== Okapi $version ==="
        
        for schema_file in "$schemas_dir"/*.schema.json; do
            [[ -f "$schema_file" ]] || continue
            [[ "$(basename "$schema_file")" == "meta.json" ]] && continue
            process_schema "$version" "$schema_file"
        done
    done
    
    # Update timestamp
    update_timestamp
    
    echo ""
    echo "Composite regeneration complete."
}

# Parse command
case "${1:-}" in
    regenerate-composites)
        regenerate_composites
        ;;
    *)
        regenerate_all
        ;;
esac
