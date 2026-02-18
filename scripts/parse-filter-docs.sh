#!/bin/bash
# Parse Okapi filter documentation using Claude CLI
# Usage: ./scripts/parse-filter-docs.sh [filter-docs-dir]
#
# Extracts structured metadata from wiki docs and outputs JSON files
# Uses Claude CLI with --json-schema for guaranteed well-formed output

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DOCS_DIR="${1:-filter-docs}"
RAW_DIR="$DOCS_DIR/raw"
PARSED_DIR="$DOCS_DIR/parsed"
JSON_SCHEMA="$SCRIPT_DIR/filter-doc-schema.json"

# Check prerequisites
if ! command -v claude &> /dev/null; then
    echo "Error: 'claude' CLI not found. Install with: npm install -g @anthropic-ai/claude-code"
    exit 1
fi

if [ ! -d "$RAW_DIR" ]; then
    echo "Error: $RAW_DIR not found. Run 'make download-filter-docs' first."
    exit 1
fi

if [ ! -f "$JSON_SCHEMA" ]; then
    echo "Error: $JSON_SCHEMA not found."
    exit 1
fi

mkdir -p "$PARSED_DIR"

# Read schema for Claude CLI
SCHEMA_JSON=$(cat "$JSON_SCHEMA" | jq -c .)

# Mapping from wiki filename to filter IDs
# Some filters have multiple IDs (configurations)
# Note: bash associative arrays don't like hyphens, so we use a function
get_filter_ids() {
    local basename="$1"
    case "$basename" in
        "archive-filter") echo "okf_archive" ;;
        "dtd-filter") echo "okf_dtd" ;;
        "doxygen-filter") echo "okf_doxygen" ;;
        "epub-filter") echo "okf_epub" ;;
        "html-filter") echo "okf_html" ;;
        "html5-its-filter") echo "okf_html5" ;;
        "icml-filter") echo "okf_icml" ;;
        "idml-filter") echo "okf_idml" ;;
        "json-filter") echo "okf_json" ;;
        "markdown-filter") echo "okf_markdown" ;;
        "message-format-filter") echo "okf_messageformat" ;;
        "mif-filter") echo "okf_mif" ;;
        "moses-text-filter") echo "okf_mosestext" ;;
        "multi-parsers-filter") echo "okf_multiparsers" ;;
        "openoffice-filter") echo "okf_odf" ;;
        "openxml-filter") echo "okf_openxml" ;;
        "pdf-filter") echo "okf_pdf" ;;
        "pensieve-tm-filter") echo "okf_pensieve" ;;
        "php-content-filter") echo "okf_phpcontent" ;;
        "plain-text-filter") echo "okf_plaintext okf_baseplaintext okf_paraplaintext" ;;
        "po-filter") echo "okf_po" ;;
        "properties-filter") echo "okf_properties" ;;
        "rainbow-translation-kit-filter") echo "okf_rainbowkit" ;;
        "regex-filter") echo "okf_regex" ;;
        "sdl-trados-package-filter") echo "okf_sdlpackage" ;;
        "simplification-filter") echo "okf_simplification" ;;
        "table-filter") echo "okf_table okf_basetable okf_commaseparatedvalues okf_tabseparatedvalues okf_fixedwidthcolumns" ;;
        "tmx-filter") echo "okf_tmx" ;;
        "trados-tagged-rtf-filter") echo "okf_tradosrtf" ;;
        "transifex-filter") echo "okf_transifex" ;;
        "ts-filter") echo "okf_ts" ;;
        "ttx-filter") echo "okf_ttx" ;;
        "txml-filter") echo "okf_txml" ;;
        "wiki-filter") echo "okf_wiki" ;;
        "wsxz-package-filter") echo "okf_wsxzpackage" ;;
        "vignette-filter") echo "okf_vignette" ;;
        "xliff-filter") echo "okf_xliff" ;;
        "xliff-2-filter") echo "okf_xliff2" ;;
        "xml-filter") echo "okf_xml" ;;
        "xml-stream-filter") echo "okf_xmlstream" ;;
        "yaml-filter") echo "okf_yaml" ;;
        *) echo "" ;;
    esac
}

# The prompt for Claude to extract structured information
EXTRACTION_PROMPT='Extract structured metadata from this Okapi Framework filter documentation (MediaWiki format).

Rules:
- Extract the filter name, description, supported formats, and file extensions
- Extract parameters from the Parameters/Options sections with name, description, type, and default values
- Include all pre-defined configurations (configId like okf_*, name, description)
- Extract code examples and sample files shown in the documentation
- Note any processing details, limitations, and related filters
- Use null for fields where information is not available
- Be precise with file extensions (include the dot)

Wiki content:'

echo "Parsing filter documentation with Claude CLI..."
echo ""

success=0
failed=0
skipped=0

for wiki_file in "$RAW_DIR"/*.wiki; do
    basename=$(basename "$wiki_file" .wiki)
    
    # Skip non-filter pages
    if [[ "$basename" == "filters" ]] || [[ "$basename" == "understanding-filter-configurations" ]] || [[ "$basename" == "inline-codes-simplifier-step" ]]; then
        continue
    fi
    
    # Get filter IDs for this wiki page
    filter_ids=$(get_filter_ids "$basename")
    if [ -z "$filter_ids" ]; then
        echo "⚠ No filter ID mapping for: $basename"
        ((skipped++))
        continue
    fi
    
    # Get the primary filter ID (first one)
    primary_id=$(echo "$filter_ids" | awk '{print $1}')
    output_file="$PARSED_DIR/${primary_id}.json"
    
    # Skip if already parsed (for incremental runs)
    if [ -f "$output_file" ] && [ "$FORCE" != "1" ]; then
        echo "⏭ Skipping $primary_id (already parsed, use FORCE=1 to reparse)"
        ((skipped++))
        continue
    fi
    
    echo -n "Parsing $basename -> $primary_id... "
    
    # Read wiki content
    wiki_content=$(cat "$wiki_file")
    
    # Construct the wiki URL
    wiki_page=$(echo "$basename" | sed 's/-/_/g' | sed 's/\b\(.\)/\u\1/g')
    wiki_url="https://okapiframework.org/wiki/index.php/${wiki_page}"
    
    # Call Claude CLI with --json-schema for guaranteed well-formed output
    full_prompt="$EXTRACTION_PROMPT

$wiki_content"
    
    if raw_result=$(echo "$full_prompt" | claude --print --dangerously-skip-permissions \
        --output-format json \
        --json-schema "$SCHEMA_JSON" \
        2>/dev/null); then
        
        # Extract structured_output from the response
        result=$(echo "$raw_result" | jq -c '.structured_output // empty')
        
        # Validate it's proper JSON and add wiki URL
        if echo "$result" | jq -e '. | type == "object"' > /dev/null 2>&1; then
            # Add/update wikiUrl and filterId fields
            echo "$result" | jq --arg url "$wiki_url" --arg id "$primary_id" \
                '. + {filterId: $id, wikiUrl: $url}' > "$output_file"
            echo "✓"
            ((success++))
            
            # Create symlinks for secondary filter IDs
            for filter_id in $filter_ids; do
                if [ "$filter_id" != "$primary_id" ]; then
                    ln -sf "${primary_id}.json" "$PARSED_DIR/${filter_id}.json" 2>/dev/null || true
                fi
            done
        else
            echo "✗ (invalid JSON)"
            echo "$result" > "$PARSED_DIR/${primary_id}.error.txt"
            ((failed++))
        fi
    else
        echo "✗ (Claude error)"
        ((failed++))
    fi
    
    # Small delay between API calls
    sleep 1
done

echo ""
echo "Parsing complete!"
echo "  Successful: $success"
echo "  Failed: $failed"
echo "  Skipped: $skipped"
echo "  Output: $PARSED_DIR/"
echo ""

# Create summary index
if [ $success -gt 0 ]; then
    echo "Creating index..."
    echo "{" > "$PARSED_DIR/index.json"
    echo '  "generatedAt": "'$(date -u +%Y-%m-%dT%H:%M:%SZ)'",' >> "$PARSED_DIR/index.json"
    echo '  "filterCount": '$success',' >> "$PARSED_DIR/index.json"
    echo '  "filters": {' >> "$PARSED_DIR/index.json"
    
    first=true
    for json_file in "$PARSED_DIR"/okf_*.json; do
        [ -L "$json_file" ] && continue  # Skip symlinks
        [ ! -f "$json_file" ] && continue
        
        filter_id=$(basename "$json_file" .json)
        filter_name=$(jq -r '.filterName // "Unknown"' "$json_file")
        
        if [ "$first" = true ]; then
            first=false
        else
            echo "," >> "$PARSED_DIR/index.json"
        fi
        printf '    "%s": "%s"' "$filter_id" "$filter_name" >> "$PARSED_DIR/index.json"
    done
    
    echo "" >> "$PARSED_DIR/index.json"
    echo "  }" >> "$PARSED_DIR/index.json"
    echo "}" >> "$PARSED_DIR/index.json"
    
    echo "Created $PARSED_DIR/index.json"
fi
