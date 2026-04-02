# Wiki Filter

The Wiki Filter extracts translatable text from wiki markup. Currently the only supported markup style is [Dokuwiki](https://www.dokuwiki.org/dokuwiki). It extracts text from headers, paragraphs, list items, image captions, and table cells/headers. All Dokuwiki syntax described in the [Dokuwiki syntax reference](https://www.dokuwiki.org/syntax) is supported for inline codes.

## Parameters

### Preserve Whitespace

Prevents the filter from collapsing whitespace in extracted text. By default, consecutive whitespace characters are collapsed into a single space. Set to `true` to preserve the original whitespace as-is.

Usage: `preserve_whitespace: true`

### Custom Inline Codes

Defines custom inline codes beyond the built-in Dokuwiki syntax. Each entry can be either:

- A **placeholder** (self-closing code) using `pattern` — a single regex matching the entire inline element
- An **opening/closing pair** using `start_pattern` and `end_pattern` — two regexes matching the start and end tags respectively

Matches are converted into inline codes within translation units. Regex patterns must match non-zero-width runs of text.

Example configuration:

```yaml
custom_codes:
  - pattern: "\[(path|menu)[^\]]*\]"
  - start_pattern: "<custom>"
    end_pattern: "</custom>"
```

## Limitations

- Attributes of inline codes (link and image URLs, etc.) are not exposed for translation or special processing.
- Embedded HTML, PHP, etc., is not extracted.

## Notes

- If the file has a Unicode Byte-Order-Mark, the corresponding encoding (UTF-8, UTF-16, etc.) is used automatically.
- If no BOM is present, the default encoding specified when opening the document is used.
- All standard Dokuwiki syntax is recognized for inline codes.

## Examples

### Dokuwiki input with extractable text

```
=== Page Title ===
Paragraph text here.
  * List item 1
    * Nested list item

{{image.jpg|Image caption}}

^ Table header 1 ^ Table header 2 |
| Table cell 1   | Table cell 2   |
```

The filter extracts: "Page Title", "Paragraph text here.", "List item 1", "Nested list item", "Image caption", "Table header 1", "Table header 2", "Table cell 1", and "Table cell 2".
