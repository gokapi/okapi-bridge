#!/bin/bash
# Merge base schema with override to create composite
# Usage: merge-schema.sh <base.json> <override.json> <output.json>
#
# If override doesn't exist, copies base as-is
# Supports $include for shared fragments (resolved before merging)
# Merges x-groups from override and field-level hints into properties

set -euo pipefail

if [[ $# -lt 3 ]]; then
    echo "Usage: $0 <base.json> <override.json> <output.json>" >&2
    exit 1
fi

BASE="$1"
OVERRIDE="$2"
OUTPUT="$3"

if [[ ! -f "$BASE" ]]; then
    echo "Error: Base file not found: $BASE" >&2
    exit 1
fi

# If no override, just copy base
if [[ ! -f "$OVERRIDE" ]]; then
    cp "$BASE" "$OUTPUT"
    exit 0
fi

OVERRIDE_DIR="$(dirname "$OVERRIDE")"

# Resolve $include references in override
# Merges all included fragments, then overlays the override's own fields/groups
resolve_override() {
    local override_file="$1"
    local override_dir="$2"
    
    # Check if override has $include
    local includes
    includes=$(jq -r '.["$include"] // empty | .[]?' "$override_file" 2>/dev/null)
    
    if [[ -z "$includes" ]]; then
        # No includes, return as-is
        cat "$override_file"
        return
    fi
    
    # Start with empty base
    local merged='{"fields":{},"groups":[]}'
    
    # Merge each included fragment
    while IFS= read -r include_path; do
        local fragment_file="$override_dir/$include_path"
        if [[ -f "$fragment_file" ]]; then
            merged=$(echo "$merged" | jq --slurpfile frag "$fragment_file" '
                # Merge fields (fragment first, then existing)
                .fields = (($frag[0].fields // {}) + .fields) |
                # Append groups (avoiding duplicates by id)
                .groups = (
                    (.groups + ($frag[0].groups // [])) |
                    group_by(.id) |
                    map(.[0])
                )
            ')
        fi
    done <<< "$includes"
    
    # Overlay the override's own fields and groups on top
    echo "$merged" | jq --slurpfile ov "$override_file" '
        # Merge fields (override wins)
        .fields = (.fields + (($ov[0].fields // {}) | del(.["$include"]))) |
        # Append/update groups from override
        .groups = (
            (.groups + ($ov[0].groups // [])) |
            group_by(.id) |
            map(if length > 1 then .[1] else .[0] end)
        ) |
        # Copy through any other top-level keys from override
        . + ($ov[0] | del(.fields, .groups, .["$include"], .["$schema"]))
    '
}

# Resolve includes into a temp file
RESOLVED_OVERRIDE=$(mktemp)
trap "rm -f $RESOLVED_OVERRIDE" EXIT
resolve_override "$OVERRIDE" "$OVERRIDE_DIR" > "$RESOLVED_OVERRIDE"

# Merge resolved override into base
# 1. Add x-groups from override.groups
# 2. For each field in override.fields, merge hints into properties
jq -s '
  .[0] as $base | .[1] as $override |
  $base |
  
  # Add x-groups if present in override
  (if $override.groups then . + {"x-groups": $override.groups} else . end) |
  
  # Merge field hints into properties
  if $override.fields and .properties then
    .properties |= with_entries(
      .key as $field |
      if $override.fields[$field] then
        .value |= . + (
          $override.fields[$field] |
          to_entries |
          map(
            if .key == "widget" then {"key": "x-widget", "value": .value}
            elif .key == "placeholder" then {"key": "x-placeholder", "value": .value}
            elif .key == "presets" then {"key": "x-presets", "value": .value}
            elif .key == "order" then {"key": "x-order", "value": .value}
            elif .key == "showIf" then {"key": "x-showIf", "value": .value}
            elif .key == "description" then {"key": "description", "value": .value}
            else {"key": .key, "value": .value}
            end
          ) |
          from_entries
        )
      else .
      end
    )
  else .
  end
' "$BASE" "$RESOLVED_OVERRIDE" > "$OUTPUT"
