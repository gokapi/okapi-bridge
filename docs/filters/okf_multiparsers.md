# Multi-Parsers Filter

The Multi-Parsers Filter extracts translatable text from two-level complex formats, where an outer format contains inner formats. A typical use case is a CSV file where some columns contain Markdown, some HTML, and some plain text. By default, the filter is configured to process a CSV file where all columns are translatable and treated as plain text.

## Parameters

Parameters are not yet documented for this filter.

## Limitations

- This filter is **BETA** — behavior and parameters may change in future releases.

## Notes

- If the input file has a Unicode Byte-Order-Mark (BOM), the corresponding encoding (UTF-8, UTF-16, etc.) is used automatically.
- If no BOM is present, the default encoding specified in the filter options is used.

## Examples

No examples are available for this filter.
