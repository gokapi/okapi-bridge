#!/bin/bash
# Merge base schema with override to create composite
# Usage: merge-schema.sh <base.json> <override.json> <output.json>
#
# If override doesn't exist, copies base as-is
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

# Merge override into base
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
' "$BASE" "$OVERRIDE" > "$OUTPUT"
