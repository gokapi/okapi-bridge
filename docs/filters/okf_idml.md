# IDML Filter

The IDML Filter processes Adobe InDesign IDML documents (InDesign Markup Language), an XML-based format introduced in InDesign CS4. The filter extracts text by spread, gathering stories from `<TextFrame>` and `<TextPath>` elements in spread order. IDML documents are ZIP packages (UCF) containing multiple sub-documents, each treated as a separate sub-document by the filter.

## Parameters

### General

#### Maximum Attribute Size
Controls the attribute buffer size in MB. Increase this if processing IDML documents with very large XML attributes that exceed the default 4 MB limit. Default: **4 MB**.

#### Special Character Pattern
Regex pattern matching characters that should be treated as **inline codes** rather than translatable text. The default pattern matches various Unicode special characters including zero-width spaces, zero-width non-joiners, soft hyphens, non-breaking hyphens, and BOM characters.

#### Untag XML Structures
Strips embedded XML structural information from the extracted content. Use this when the IDML document contains XML tagging that is not relevant for translation.

#### Merge Adjacent Codes
Combines consecutive inline codes into a single code, reducing tag clutter for translators. Default: **false**.

### Extraction

#### Extract Notes
Includes content from `<Note>` elements in the extraction. Default: **false**.

#### Extract Master Spreads
Includes content from master spreads in the extraction. Enable this if master spreads contain translatable text (e.g., headers, footers, recurring labels). Default: **false**.

#### Extract Hidden Layers
Includes content from hidden layers in the extraction. Default: **false**.

#### Extract Hidden Pasteboard Items
Includes content from items on the pasteboard (outside the visible page area) that are hidden. Default: **false**.

#### Extract Breaks Inline
Treats break elements as inline codes within text units rather than creating separate segments. Default: **false**.

#### Extract Hyperlink Text Sources Inline
Controls how hyperlink text sources are represented:
- **Enabled**: Hyperlink text sources are extracted as inline elements within the surrounding text.
- **Disabled** (default): Hyperlink text sources are represented as referencing groups of separate textual units.

#### Extract Custom Text Variables
Includes custom text variables defined in the InDesign document. Default: **false**.

#### Extract Index Topics
Includes index topic entries in the extraction. Default: **false**.

#### Extract External Hyperlinks
Includes external hyperlink URLs/text for translation. Enable when hyperlink destinations need to be localized. Default: **false**.

#### Extract Math Zones
Includes math zone content for translation. Default: **true**.

#### Excluded Styles
Specifies paragraph or character style names whose content should be excluded from extraction. Use this to skip non-translatable styled content such as code listings or product IDs.

### Formatting

#### Skip Discretionary Hyphens
Omits discretionary (soft) hyphens from extracted text. Default: **false**.

#### Ignore Character Kerning
Prevents kerning differences from creating separate inline codes. Default: **false**.

#### Ignore Character Tracking
Prevents tracking (letter-spacing) differences from creating separate inline codes. Default: **false**.

#### Ignore Character Leading
Prevents leading (line-spacing) differences from creating separate inline codes. Default: **false**.

#### Ignore Character Baseline Shift
Prevents baseline shift differences from creating separate inline codes. Default: **false**.

## Processing Notes

- Input encoding is **automatically detected**; user-specified encoding is ignored. Always uses UTF-8.
- Output line-breaks are always simple linefeed (LF).
- IDML documents are ZIP packages (UCF). Each embedded file is treated as a sub-document.
- Text is extracted by spread, with stories gathered from `<TextFrame>` and `<TextPath>` elements.
- Stories embedded inside other stories and not declared at spread level are extracted in a special group.
- The filter wraps `IDMLContentFilter` — the inner filter handles raw Story XML files directly.

## Limitations

- The IDML filter is marked as **ALPHA** in the Okapi help documentation.

## Examples

### Clean extraction with reduced inline codes

Minimize inline code clutter by merging adjacent codes and ignoring formatting-only differences:

```yaml
mergeAdjacentCodes: true
ignoreCharacterKerning: true
ignoreCharacterTracking: true
ignoreCharacterLeading: true
ignoreCharacterBaselineShift: true
```

### Full content extraction

Extract all content including hidden layers, master spreads, and notes:

```yaml
extractMasterSpreads: true
extractHiddenLayers: true
extractNotes: true
extractCustomTextVariables: true
extractIndexTopics: true
```
