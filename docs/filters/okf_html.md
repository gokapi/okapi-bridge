# HTML Filter

The HTML Filter implements the `IFilter` interface for **HTML and XHTML** documents. It processes character and numeric entities (converting them to Unicode), handles encoding detection from document declarations, and preserves original line-break types. The filter is primarily configured through YAML configuration files that define element rules, translatable attributes, and structural groupings.

## Parameters

### Built-in Configuration

The HTML filter uses a minimalist configuration by default that does not create structural groupings (no table groups, list groups, etc.). The predefined `okf_html-wellFormed` configuration enables structural groupings, but requires all structural tags to be well-formed with matching start and end tags.

Custom YAML configuration files can define element rules (`INLINE`, `GROUP`, `EXCLUDE`, `INCLUDE`, `TEXTUNIT`, `PRESERVE_WHITESPACE`, `ATTRIBUTES_ONLY`), translatable attributes with optional conditions, and other processing options.

**Important:** All element and attribute names in configuration files must be **lowercase**. Prefixed attributes (e.g., `xml:lang`) must be enclosed in single quotes.

### Inline Code Finder

#### useCodeFinder
Activates the inline code finder, which uses regular expressions defined in `codeFinderRules` to identify spans of extracted text that should be protected as inline codes.

#### codeFinderRules
Defines regular expression rules for identifying inline codes. Uses the Okapi `#v1` code finder format. Backslashes must be double-escaped in single-line YAML notation; use block scalar (`|-`) syntax to avoid this.

### Output Options

#### escapeCharacters
A string containing all characters that should be output as HTML named character entity references. By default, extended characters are output as-is. Characters without a defined HTML entity are output normally.

#### quoteModeDefined
Must be set to `true` to activate the custom `quoteMode` setting.

#### quoteMode
Controls how quote and apostrophe characters are escaped in output:
- **0** (UNESCAPED) — No escaping
- **1** (ALL) — Escape both to named entities
- **2** (NUMERIC_SINGLE_QUOTES) — Named entity for double quotes, numeric for single
- **3** (DOUBLE_QUOTES_ONLY) — Escape double quotes only

Requires `quoteModeDefined: true`.

### Processing Options

#### inlineCdata
Treats `<![CDATA[...]]>` sections as inline elements rather than breaking text flow.

#### cleanupHtml
Controls automatic cleanup of common HTML syntax errors (e.g., unquoted attributes). Set to `false` to disable.

## Limitations

- Content of `<style>` and `<script>` elements is not extracted.
- Tags from server-side scripts (PHP, ASPX, JSP, etc.) are not formally supported and are treated as non-translatable.

## Notes

- If the document has an encoding declaration, it is used; otherwise the default encoding from filter options applies.
- For UTF-8 output, a BOM is included only if the input was also UTF-8 and had a BOM.
- If the input has no declared encoding, the filter tries to add a `<meta>` (HTML) or `<meta />` (XHTML) tag in output when a `<head>` element exists.
- Output line-breaks match the original input.
- Character and numeric entities are converted to Unicode; DTD/schema-defined entities pass through unchanged.
- Text entity declarations can be processed by the DTD Filter.

## Examples

### Inline Code Finder for Variables

Protects `VAR1`, `VAR2`, etc. from translation:

```yaml
useCodeFinder: true
codeFinderRules: |-
    #v1
    count.i=1
    rule0=\bVAR\d\b
```

Input: `<p>Number of files = VAR1</p>`

### Character Entity References

```yaml
escapeCharacters: "\u00a9 \u20ac\u00b5\u00c6"
```

Input: `<p>\u00a9 \u20ac\u00b5\u00c6\u0104</p>` \u2192 Output: `<p>&amp;copy;&amp;nbsp;&amp;euro;&amp;micro;&amp;AElig;\u0104</p>`

### Exclude-by-Default

```yaml
exclude_by_default: true
elements:
    title:
      ruleTypes: [TEXTUNIT]
```

Only the `<title>` element content is extracted for translation.
