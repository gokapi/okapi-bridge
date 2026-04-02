# Markdown Filter

The Markdown Filter extracts translatable text from Markdown files based on the [CommonMark](http://commonmark.org) specification, with additional support for [GitHub-flavored Markdown](https://guides.github.com/features/mastering-markdown/). HTML elements within Markdown are processed by the HTML filter (configurable separately). The filter supports inline code finding, subfilter delegation for HTML and YAML content, and various options to control which Markdown constructs are exposed for translation.

## Parameters

### URL Translation

#### Translate Hyperlink URLs (`translateUrls`)
URLs in link (`[text](url)`) and image (`![alt](url)`) statements are normally treated as non-translatable. Enabling this extracts URLs as **subflows** — they appear as separate translation units *before* the segment that references them.

**Note:** When subflows are extracted, the subflow translation unit appears **before** the segment containing the link in the XLIFF file.

#### Translatable URL Pattern (`urlToTranslatePattern`)
Restricts which URLs are extracted for translation when `translateUrls` is enabled. Only URLs matching this Java regular expression will be extracted as subflows. Default: `.+` (all URLs).

Only takes effect when `translateUrls` is true.

### Code Block Translation

#### Translate Fenced Code Blocks (`translateCodeBlocks`)
Controls whether the contents of fenced code blocks (delimited by ` ``` ` or `~~~`) are exposed for translation. Default: true.

#### Translate Indented Code Blocks (`translateIndentedCodeBlocks`)
Controls whether the contents of indented code blocks (lines indented by 4+ spaces or 1+ tab) are exposed for translation. Default: true.

#### Translate Inline Code Blocks (`translateInlineCodeBlocks`)
Controls whether text delimited by single backticks is exposed for translation. Default: true.

### Content Extraction

#### Translate Image Alt Text (`translateImageAltText`)
Extracts alt text from images for translation. Applies to both Markdown image syntax (`![alt text](url)`) and HTML `<img>` tags with `alt` attributes. Default: true.

#### Generate Header Anchors (`generateHeaderAnchors`)
Automatically generates explicit named anchors for headings using the `{#my-anchor}` syntax. This provides stable anchors for hyperlinks that reference translatable header text. Default: false.

#### Non-Translatable Block Quotes (`nonTranslateBlocks`)
Prevents specific block quotes from being extracted for translation. Block quotes whose content starts with one of the specified comma-separated strings will be skipped entirely. Default: empty (all block quotes extracted).

### MDX Support

#### Parse MDX Expressions (`parseMdx`)
Parses out multi-line `export` blocks as skeleton (non-translatable). Useful for MDX files that mix Markdown with JSX/JavaScript.

**Warning:** This feature is **experimental** and uses regex-based parsing.

### Character Escaping

#### HTML Entities to Escape (`htmlEntitesToEscape`)
Specifies characters that should be encoded as HTML entities on export. Enter the raw characters as a single string. Default: empty.

#### Support Backslash Escaping (`unescapeBackslashCharacters`)
Enables parsing of backslash-escaped punctuation in source documents. On export, characters listed in `charactersToEscape` will be re-escaped. Default: false.

#### Characters to Escape (`charactersToEscape`)
Lists the punctuation characters that will be backslash-escaped on export. Only applies when `unescapeBackslashCharacters` is enabled. Default: ``*_`{}[]<>()#+\-.!|``

### Subfilters

#### HTML Subfilter Configuration (`htmlSubfilter`)
Custom HTML filter configuration ID for processing HTML elements within Markdown. The configuration file must be saved with a `.fprm` suffix. Leave empty for the default configuration tailored for Markdown.

**Note:** The Markdown filter's own Inline Code Finder does not apply to HTML inline tags or HTML blocks — configure inline codes for HTML content in the HTML subfilter.

#### YAML Subfilter Configuration (`yamlSubfilter`)
Custom YAML filter configuration ID for processing YAML metadata headers. Allows fine-grained control over which metadata fields are extracted. Default: empty.

### Inline Code Finder

#### Use Code Finder (`useCodeFinder`)
Enables the Inline Code Finder, which uses regex patterns to identify inline codes within translatable text. Matched patterns are treated as non-translatable placeholders within segments.

**Note:** This applies only to the Markdown portion — for HTML content within Markdown, configure inline codes in the HTML subfilter separately. Inline Code Finder support was temporarily unavailable in some snapshot builds of version 0.36 but has been restored.

## Limitations

- Designed for CommonMark-based Markdown; not all Markdown dialects are fully supported.
- MDX parsing (`parseMdx`) is experimental and uses regex-based extraction.
- The wiki documents `translateImageAltText` as the parameter name for both the YAML metadata header and image alt text options — this appears to be a documentation error.

## Notes

- If the file has a Unicode BOM, the corresponding encoding is used automatically; otherwise the default encoding from filter options is used.
- HTML inline elements and HTML blocks within Markdown are delegated to the HTML filter for processing.
- URLs extracted for translation appear as subflows — separate translation units extracted before the referencing segment.

## Examples

### URL Translation as Subflows

When `translateUrls` is enabled, URLs in image/link statements are extracted as separate translation units:

**Input:**
```markdown
Please click ![The Information desk logo](images/circled-i.jpg) for help.
```

**Output (XLIFF):**
```xml
<trans-unit id="tu2" restype="x-img-link" xml:space="preserve">
  <source xml:lang="en">images/circled-i.jpg</source>
</trans-unit>
<trans-unit id="tu1" xml:space="preserve">
  <source xml:lang="en">Please click <bpt id="1">![</bpt>The Information desk logo<ept id="1">]([#$tu2])</ept> for help.</source>
</trans-unit>
```
