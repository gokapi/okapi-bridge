# OpenXML (Microsoft Office) Filter

Processes Microsoft Office documents from Office 2007 and later, including DOCX (Word), XLSX (Excel), and PPTX (PowerPoint). These documents use the OpenXML (ZIP-based) format, as opposed to the binary formats used by pre-2007 versions of Office. The filter provides format-specific extraction options for each document type, with support for comments, hidden content, document properties, charts, diagrams, and color-based exclusion.

Supported extensions: `.docx`, `.docm`, `.dotx`, `.dotm`, `.pptx`, `.pptm`, `.ppsx`, `.ppsm`, `.potx`, `.potm`, `.xlsx`, `.xlsm`, `.xltx`, `.xltm`, `.vsdx`, `.vsdm`

## Parameters

### General Options

#### Aggressive Cleanup
Strips additional formatting tags related to text spacing. Improves filtering quality when Office documents were **converted from other formats** (particularly PDF), where imperfect conversion adds extra formatting noise.

#### Ignore Whitespace Styles
Whitespace character styles (formatting) are ignored and considered equal to the consequential ones during tag cleanup. Requires **Aggressive Cleanup** to be enabled.

#### Preserve Font Categories on Detection
Preserves the ASCII and HighAnsi run font categories when merging consequential runs.

#### Remove Embedded Excel Package
Removes the embedded Excel data package and its references from chart parts. Only relevant when cached chart strings or numbers are set for extraction.

#### Extract External Hyperlinks
Exposes external hyperlink URLs for translation.

#### Include/Exclude Mode (Styles)
Controls the style-based include/exclude mechanism. In **Exclude** mode, text with specified styles is skipped. In **Include** mode, only text with specified styles is extracted.

#### Include/Exclude Mode (Highlight Colors)
Controls the highlight-color-based include/exclude mechanism. In **Include** mode, only text with specified highlight colors is extracted. In **Exclude** mode, all content except highlighted text is extracted.

> **Note:** Text excluded via highlight colors is treated as hidden — enabling "Translate Hidden" options will still extract it.
> **Note:** Starting in 1.48.0, this option also applies to PowerPoint content.

#### Add Line Separator as Character
Adds line separator characters (instead of inline codes) for line breaks within paragraphs.

#### Add Tab as Character
Adds tab characters (instead of inline codes) for tabs within text runs.

#### Allow Empty Targets
Allows empty target segments during merge. Without this, empty targets may be skipped or filled with source text.

#### Ignore Soft Hyphen Tags
Ignores soft hyphen tags in the document, removing them from extracted content.

#### Replace No-Break Hyphen Tags
Replaces no-break hyphen tags with actual no-break hyphen characters.

#### Line Separator Replacement
The string used to replace line separator elements. Default: `
`.

#### Max Attribute Size
Maximum size in bytes for XML attributes. Default: 4194304 (4 MB). Increase for documents with very large embedded attribute data.

#### File Type
Identifies the document type being processed: `MSWORD`, `MSEXCEL`, `MSWORDDOCPROPERTIES`, `MSPOWERPOINTCOMMENTS`. Normally set automatically by file extension.

### Word Options

#### Translate Headers and Footers
Exposes header and footer content for translation. Default: on.

#### Translate Hidden Text
Exposes hidden text for translation. Also extracts text excluded via highlight colors. Default: on.

#### Translate Comments
Exposes document comments for translation. Default: on.

#### Translate Document Properties
Exposes title, subject, creator, description, category, keywords, and content status for translation. Default: on.

#### Exclude Graphic Metadata
Excludes `@name` and `@descr` attributes of drawings and word art from extraction.

#### Translate Graphic Name
Exposes `@name` attribute values of drawings and word art for translation. Default: on.

#### Translate Graphic Description
Exposes `@descr` attribute values of drawings and word art for translation. Default: off.

#### Ignore Font Colors
Ignores font colours during processing. When combined with aggressive cleanup and empty thresholds, font colour properties are **permanently removed** from the document.

#### Font Colors Min Threshold
Font colours are ignored starting from this value. Accepts preset names (`black`) or RGB hex (`000000`). Empty defaults to white.

#### Font Colors Max Threshold
Font colours are ignored up to this value. Accepts preset names (`white`) or RGB hex (`FFFFFF`). Empty defaults to white.

#### Translate Numbering Level Text
Exposes numbering-level text (list format strings) for translation. Default: off.

#### Allow Style Optimisation
Common formatting of all runs in a paragraph is moved to the styles part. Default: on.

#### Automatically Accept Revisions
Accepts tracked changes before processing. Default: on.

### Excel Options

#### Translate Hidden
Exposes hidden rows and columns for translation. Default: off.

#### Translate Sheet Names
Exposes worksheet tab names for translation. Default: off.

#### Translate Drawings
Exposes text from shapes and drawings for translation. Default: off.

#### Translate Diagram Data
Exposes SmartArt and diagram data for translation. Default: off.

#### Exclude Colors
Excludes cells matching selected foreground/background colors. UI shows the standard Excel 2010 palette. Custom RGB values (`RRGGBB`) can be added by editing the `.fprm` config file.

#### Translate Cells Copied
Copies cell data on extraction for contextualised, independent translations. Default: on.

#### Preserve Styles in Target Columns
Preserves existing cell styles in target columns. Default: off.

#### Join Source and Target Columns
Joins source and target columns during extraction. Default: off.

#### Extract Only Specified Worksheets
Only extracts worksheets matching Worksheet Configuration name patterns. Default: off.

#### Extract Only Specified Cells
Only extracts cells specified in Worksheet Configurations. Default: off.

#### Apply Source Column Styles for Exclusion
Uses source column styles when evaluating color-based exclusion rules. Default: off.

### PowerPoint Options

#### Translate Hidden Slides
Exposes hidden slides for translation. Default: off.

#### Translate Masters
Exposes slide masters, notes masters, and layouts in use by at least one slide. Default: on.

#### Translate Notes
Exposes speaker notes for translation. Default: off.

#### Translate Charts
Exposes chart text for translation. Default: on.

#### Translate Comments
Exposes slide comments for translation. Currently coupled with the General Options comment setting — both must be enabled. Default: on.

#### Translate Document Properties
Exposes document properties for translation. Currently coupled with the General Options setting — both must be enabled. Default: on.

#### Translate Diagram Data
Exposes SmartArt and diagram data for translation. Default: on.

#### Translate Graphic Name / Description
Exposes `@name` and `@descr` attributes of drawings and word art. Name default: on, Description default: off.

#### Translate Cached Chart Strings
Exposes pre-rendered chart string values. Default: off.

#### Translate Cached Chart Numbers
Exposes cached chart numbers and format codes. Default: off.

#### Only Included Slide Numbers
Restricts extraction to specific slide numbers. Default: off.

#### Ignore Placeholders in Masters
Prevents placeholder content in masters from being extracted. Default: off.

#### Reorder Options
Multiple reorder options control the ordering of parts within the output ZIP archive:
- **Reorder Notes** — after slide and chart parts
- **Reorder Charts** — after slide/layout/master and diagram parts
- **Reorder Comments** — after slide and note parts
- **Reorder Diagram Data** — after slide/layout/master and relationship parts
- **Reorder Document Properties** — after `_rels/.rels`
- **Reorder Relationships** — after related slide/layout/master
- **Reorder Notes and Comments** — both after slide parts

### Inline Codes

#### Enabled
Enable pattern-based detection of inline codes (placeholders, tags, etc.).

#### Rules
Regex patterns for detecting inline codes within text. Presets available for merge fields (`«FirstName»`) and placeholders (`{name}`).

#### Merge Adjacent
Merge consecutive inline codes into a single placeholder.

#### Move Boundary Codes
Move inline codes at segment boundaries to the skeleton (non-translatable).

## Limitations

- Various known issues tracked in the [Okapi Bitbucket issue tracker](https://bitbucket.org/okapiframework/okapi/issues?status=new&title=~OpenXML).
- Pre-2007 binary Office formats (`.doc`, `.xls`, `.ppt`) are **not** supported.
- PowerPoint comments and document properties extraction is currently coupled with the General Options equivalents.

## Notes

- The highlight-color include/exclude mechanism treats excluded text as hidden, so "Translate Hidden" options can override highlight-based exclusions.
- Starting in 1.48.0, Word highlight color exclusion/inclusion also applies to PowerPoint content.
- Excel worksheet configurations use `java.util.regex.Pattern` syntax for name patterns and ALPHA-26 notation (A, B, C…) for columns.
- Custom Excel exclusion colors can use RGB hex format (`RRGGBB`) via manual config editing.

## Examples

### Excel worksheet with source/target columns

Configure extraction to translate column A into column B, excluding header rows 1-2 and columns C-D:

```
worksheetConfigurations.number.i=1
worksheetConfigurations.0.namePattern=Sheet1
worksheetConfigurations.0.sourceColumns=A
worksheetConfigurations.0.targetColumns=B
worksheetConfigurations.0.excludedRows=1,2
worksheetConfigurations.0.excludedColumns=C,D
```

### Excel worksheet with metadata columns

Translate columns A and B, treat column D as row metadata, rows 1-2 as column headers, exclude row 5 and column C:

```
worksheetConfigurations.number.i=1
worksheetConfigurations.0.namePattern=Sheet1
worksheetConfigurations.0.excludedRows=5
worksheetConfigurations.0.excludedColumns=C
worksheetConfigurations.0.metadataRows=1,2
worksheetConfigurations.0.metadataColumns=D
```

### Excluding custom Excel colors

Exclude Pantone 292 (#69b3e7) by editing the config file:

```
tsExcelExcludedColors.i=1
ccc0=69b3e7
```
