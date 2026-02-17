#!/bin/bash
# Compute canonical hash of a JSON file
# Usage: compute-hash.sh <file.json>
#
# Outputs 12-character hash (first 6 bytes of SHA-256)
# Uses canonical JSON (sorted keys, compact) for stable hashing

set -euo pipefail

if [[ $# -lt 1 ]]; then
    echo "Usage: $0 <file.json>" >&2
    exit 1
fi

FILE="$1"

if [[ ! -f "$FILE" ]]; then
    echo "Error: File not found: $FILE" >&2
    exit 1
fi

# Convert to canonical JSON (sorted keys, compact) and hash
jq -cS '.' "$FILE" | shasum -a 256 | cut -c1-12
