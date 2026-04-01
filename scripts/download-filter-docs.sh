#!/bin/bash
# Download Okapi filter and step documentation from the wiki
# Usage: ./scripts/download-filter-docs.sh [output-dir]
#
# Downloads raw wiki content for each filter and step to enable metadata enrichment

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

# Step pages from https://okapiframework.org/wiki/index.php/Steps
STEP_PAGES=(
    "Batch_Translation_Step"
    "BOM_Conversion_Step"
    "Character_Count_Step"
    "Cleanup_Step"
    "Copy_Or_Move_Step"
    "Create_Target_Step"
    "Desegmentation_Step"
    "Diff_Leverage_Step"
    "Encoding_Conversion_Step"
    "Enrycher_Step"
    "External_Command_Step"
    "Extraction_Verification_Step"
    "Filter_Events_to_Raw_Document_Step"
    "Format_Conversion_Step"
    "Full-Width_Conversion_Step"
    "Id-Based_Copy_Step"
    "Id-Based_Aligner_Step"
    "Image_Modification_Step"
    "Inconsistency_Check_Step"
    "Inline_Codes_Removal_Step"
    "Inline_Codes_Simplifier_Step"
    "Leveraging_Step"
    "Line-Break_Conversion_Step"
    "Localizables_Check_Step"
    "Microsoft_Batch_Translation_Step"
    "Paragraph_Alignment_Step"
    "Post-segmentation_Inline_Codes_Removal_Step"
    "Quality_Check_Step"
    "Raw_Document_to_Filter_Events_Step"
    "Rainbow_Translation_Kit_Creation_Step"
    "Rainbow_Translation_Kit_Merging_Step"
    "RTF_Conversion_Step"
    "Remove_Target_Step"
    "Repetition_Analysis_Step"
    "Resource_Simplifier_Step"
    "Scoping_Report_Step"
    "Search_and_Replace_Step"
    "Segmentation_Step"
    "Segments_to_Text_Units_Converter_Step"
    "Sentence_Alignment_Step"
    "Simple_Word_Count_Step"
    "Space_Check_Step"
    "Term_Extraction_Step"
    "Terminology_Leveraging_Step"
    "Text_Modification_Step"
    "TM_Import_Step"
    "Tokenization_Step"
    "Translation_Comparison_Step"
    "URI_Conversion_Step"
    "Used_Characters_Listing_Step"
    "Word_Count_Step"
    "Whitespace_Correction_Step"
    "TTX_Joiner_Step"
    "TTX_Splitter_Step"
    "XLIFF_Joiner_Step"
    "XLIFF_Splitter_Step"
    "XML_Analysis_Step"
    "XML_Characters_Fixing_Step"
    "XML_Validation_Step"
    "XSL_Transformation_Step"
)

# Additional useful pages
ADDITIONAL_PAGES=(
    "Filters"
    "Steps"
    "Understanding_Filter_Configurations"
)

mkdir -p "$OUTPUT_DIR/raw"
mkdir -p "$OUTPUT_DIR/raw/steps"
mkdir -p "$OUTPUT_DIR/parsed"

echo "Downloading Okapi documentation to $OUTPUT_DIR/"
echo ""

# Download index pages
echo "Downloading index pages..."
curl -sL "${WIKI_BASE}/Filters" > "$OUTPUT_DIR/raw/index.html"
curl -sL "${WIKI_BASE}/Steps" > "$OUTPUT_DIR/raw/steps-index.html"

# Download raw wikitext for a wiki page
download_wiki_page() {
    local page="$1"
    local output_file="$2"

    # Get raw wikitext via API (action=raw)
    local raw_url="${WIKI_BASE}/${page}?action=raw"
    local html_url="${WIKI_BASE}/${page}"

    # Download raw wikitext
    if curl -sfL "$raw_url" > "$output_file.wiki" 2>/dev/null; then
        echo "  ✓ $page (wikitext)"
    else
        # Fallback to HTML if raw not available
        if curl -sfL "$html_url" > "$output_file.html" 2>/dev/null; then
            echo "  ✓ $page (html fallback)"
        else
            echo "  ✗ $page (not found)"
            return 1
        fi
    fi
}

echo ""
echo "Downloading filter documentation..."
filter_success=0
filter_failed=0

for page in "${FILTER_PAGES[@]}"; do
    # Convert page name to filename (Archive_Filter -> archive-filter)
    filename=$(echo "$page" | tr '[:upper:]' '[:lower:]' | tr '_' '-')
    if download_wiki_page "$page" "$OUTPUT_DIR/raw/$filename"; then
        ((filter_success++))
    else
        ((filter_failed++))
    fi
    sleep 0.2
done

echo ""
echo "Downloading step documentation..."
step_success=0
step_failed=0

for page in "${STEP_PAGES[@]}"; do
    filename=$(echo "$page" | tr '[:upper:]' '[:lower:]' | tr '_' '-')
    if download_wiki_page "$page" "$OUTPUT_DIR/raw/steps/$filename"; then
        ((step_success++))
    else
        ((step_failed++))
    fi
    sleep 0.2
done

echo ""
echo "Downloading additional pages..."
for page in "${ADDITIONAL_PAGES[@]}"; do
    filename=$(echo "$page" | tr '[:upper:]' '[:lower:]' | tr '_' '-')
    download_wiki_page "$page" "$OUTPUT_DIR/raw/$filename" || true
    sleep 0.2
done

# --- Copy local Okapi help HTML (if OKAPI_SOURCE is set) ---

OKAPI_SOURCE="${OKAPI_SOURCE:-}"
html_filter_count=0
html_step_count=0

if [ -d "${OKAPI_SOURCE}/help" ]; then
    echo ""
    echo "Copying local Okapi help HTML from $OKAPI_SOURCE/help/..."

    mkdir -p "$OUTPUT_DIR/raw/html/filters"
    mkdir -p "$OUTPUT_DIR/raw/html/steps"

    # Copy filter help HTML (help/filters/{name}/ui/index.html → html/filters/{name}.html)
    for filter_dir in "$OKAPI_SOURCE"/help/filters/*/; do
        [ -d "$filter_dir" ] || continue
        filter_name=$(basename "$filter_dir")
        if [ -f "$filter_dir/ui/index.html" ]; then
            # Concatenate all HTML files in the filter's ui/ directory
            cat "$filter_dir"/ui/*.html > "$OUTPUT_DIR/raw/html/filters/${filter_name}.html"
            echo "  ✓ filter: $filter_name"
            ((html_filter_count++))
        fi
    done

    # Copy step help HTML (help/steps/{name}step.html → html/steps/{name}step.html)
    for step_html in "$OKAPI_SOURCE"/help/steps/*.html; do
        [ -f "$step_html" ] || continue
        step_name=$(basename "$step_html")
        [ "$step_name" = "index.html" ] && continue
        cp "$step_html" "$OUTPUT_DIR/raw/html/steps/$step_name"
        echo "  ✓ step: $step_name"
        ((html_step_count++))
    done

    # Copy shared help files that multiple steps reference
    if [ -f "$OKAPI_SOURCE/help/lib/ui/verification/parameterseditor.html" ]; then
        cp "$OKAPI_SOURCE/help/lib/ui/verification/parameterseditor.html" \
           "$OUTPUT_DIR/raw/html/steps/parameterseditor.html"
        echo "  ✓ shared: parameterseditor.html (quality check params)"
    fi

    echo "  HTML: $html_filter_count filters, $html_step_count steps"
else
    if [ -n "$OKAPI_SOURCE" ]; then
        echo ""
        echo "Warning: OKAPI_SOURCE=$OKAPI_SOURCE but help/ directory not found"
    fi
fi

# Create manifest with download metadata
cat > "$OUTPUT_DIR/manifest.json" << EOF
{
  "downloadedAt": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "source": "$WIKI_BASE",
  "filterCount": $filter_success,
  "filterFailed": $filter_failed,
  "stepCount": $step_success,
  "stepFailed": $step_failed,
  "htmlFilterCount": $html_filter_count,
  "htmlStepCount": $html_step_count,
  "okapiSource": "${OKAPI_SOURCE:-null}",
  "filters": [
$(printf '    "%s"' "${FILTER_PAGES[0]}")
$(printf ',\n    "%s"' "${FILTER_PAGES[@]:1}")
  ],
  "steps": [
$(printf '    "%s"' "${STEP_PAGES[0]}")
$(printf ',\n    "%s"' "${STEP_PAGES[@]:1}")
  ]
}
EOF

echo ""
echo "Download complete!"
echo "  Filters: $filter_success downloaded, $filter_failed failed"
echo "  Steps:   $step_success downloaded, $step_failed failed"
echo "  Output:  $OUTPUT_DIR/"
echo ""
echo "Files created:"
echo "  $OUTPUT_DIR/raw/*.wiki              - Filter wikitext source"
echo "  $OUTPUT_DIR/raw/steps/*.wiki        - Step wikitext source"
if [ $html_filter_count -gt 0 ] || [ $html_step_count -gt 0 ]; then
echo "  $OUTPUT_DIR/raw/html/filters/*.html - Filter help HTML (from Okapi source)"
echo "  $OUTPUT_DIR/raw/html/steps/*.html   - Step help HTML (from Okapi source)"
fi
echo "  $OUTPUT_DIR/manifest.json           - Download metadata"
echo ""
echo "Next: Run 'make parse-filter-docs' to extract structured data"
