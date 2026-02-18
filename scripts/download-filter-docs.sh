#!/bin/bash
# Download Okapi filter documentation from the wiki
# Usage: ./scripts/download-filter-docs.sh [output-dir]
#
# Downloads raw wiki content for each filter to enable metadata enrichment

set -e

OUTPUT_DIR="${1:-filter-docs}"
WIKI_BASE="https://okapiframework.org/wiki/index.php"
WIKI_API="https://okapiframework.org/wiki/api.php"

# List of filter pages from the wiki
FILTER_PAGES=(
    "Archive_Filter"
    "DTD_Filter"
    "Doxygen_Filter"
    "EPUB_Filter"
    "HTML_Filter"
    "HTML5-ITS_Filter"
    "ICML_Filter"
    "IDML_Filter"
    "JSON_Filter"
    "Markdown_Filter"
    "Message_Format_Filter"
    "MIF_Filter"
    "Moses_Text_Filter"
    "Multi-Parsers_Filter"
    "OpenOffice_Filter"
    "OpenXML_Filter"
    "PDF_Filter"
    "Pensieve_TM_Filter"
    "PHP_Content_Filter"
    "Plain_Text_Filter"
    "PO_Filter"
    "Properties_Filter"
    "Rainbow_Translation_Kit_Filter"
    "Regex_Filter"
    "SDL_Trados_Package_Filter"
    "Simplification_Filter"
    "Table_Filter"
    "TMX_Filter"
    "Trados-Tagged_RTF_Filter"
    "Transifex_Filter"
    "TS_Filter"
    "TTX_Filter"
    "TXML_Filter"
    "Wiki_Filter"
    "WSXZ_Package_Filter"
    "Vignette_Filter"
    "XLIFF_Filter"
    "XLIFF-2_Filter"
    "XML_Filter"
    "XML_Stream_Filter"
    "YAML_Filter"
)

# Additional useful pages
ADDITIONAL_PAGES=(
    "Filters"
    "Understanding_Filter_Configurations"
    "Inline_Codes_Simplifier_Step"
)

mkdir -p "$OUTPUT_DIR/raw"
mkdir -p "$OUTPUT_DIR/parsed"

echo "Downloading Okapi filter documentation to $OUTPUT_DIR/"
echo ""

# Download index page
echo "Downloading filter index..."
curl -sL "${WIKI_BASE}/Filters" > "$OUTPUT_DIR/raw/index.html"

# Download raw wikitext for each filter using MediaWiki API
download_wiki_page() {
    local page="$1"
    local output_base="$2"
    
    # Get raw wikitext via API (action=raw)
    local raw_url="${WIKI_BASE}/${page}?action=raw"
    local html_url="${WIKI_BASE}/${page}"
    
    # Download raw wikitext
    if curl -sfL "$raw_url" > "$OUTPUT_DIR/raw/${output_base}.wiki" 2>/dev/null; then
        echo "  ✓ $page (wikitext)"
    else
        # Fallback to HTML if raw not available
        if curl -sfL "$html_url" > "$OUTPUT_DIR/raw/${output_base}.html" 2>/dev/null; then
            echo "  ✓ $page (html fallback)"
        else
            echo "  ✗ $page (not found)"
            return 1
        fi
    fi
}

echo ""
echo "Downloading filter documentation..."
success=0
failed=0

for page in "${FILTER_PAGES[@]}"; do
    # Convert page name to filename (Archive_Filter -> archive-filter)
    filename=$(echo "$page" | tr '[:upper:]' '[:lower:]' | tr '_' '-')
    if download_wiki_page "$page" "$filename"; then
        ((success++))
    else
        ((failed++))
    fi
    # Small delay to be nice to the wiki server
    sleep 0.2
done

echo ""
echo "Downloading additional pages..."
for page in "${ADDITIONAL_PAGES[@]}"; do
    filename=$(echo "$page" | tr '[:upper:]' '[:lower:]' | tr '_' '-')
    download_wiki_page "$page" "$filename" || true
    sleep 0.2
done

# Create manifest with download metadata
cat > "$OUTPUT_DIR/manifest.json" << EOF
{
  "downloadedAt": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "source": "$WIKI_BASE",
  "filterCount": $success,
  "failedCount": $failed,
  "filters": [
$(printf '    "%s"' "${FILTER_PAGES[0]}")
$(printf ',\n    "%s"' "${FILTER_PAGES[@]:1}")
  ]
}
EOF

echo ""
echo "Download complete!"
echo "  Successful: $success"
echo "  Failed: $failed"
echo "  Output: $OUTPUT_DIR/"
echo ""
echo "Files created:"
echo "  $OUTPUT_DIR/raw/*.wiki     - Raw wikitext source"
echo "  $OUTPUT_DIR/manifest.json  - Download metadata"
echo ""
echo "Next: Run 'make parse-filter-docs' to extract structured data"
