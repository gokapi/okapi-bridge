# EPUB Filter

Processes EPUB (Electronic Publication) documents for localization. The filter handles encoding detection automatically, supporting Unicode BOM detection and configurable default encoding. At this time, the filter does not have a dedicated parameter editor — custom configurations must be edited manually in a text editor.

## Parameters

This filter does not currently expose configurable parameters through a UI editor. Custom configurations must be edited with a text editor.

## Encoding Behavior

### Input Encoding

The filter decides which encoding to use for the input file using the following logic:

- If the file has a Unicode Byte-Order-Mark, the corresponding encoding (e.g. UTF-8, UTF-16) is used.
- Otherwise, the default encoding specified in the filter options is used.

### Output Encoding

If the output encoding is UTF-8:

- If the input encoding was also UTF-8, a BOM is written only if one was detected in the input document.
- If the input encoding was not UTF-8, no BOM is written in the output document.

## Limitations

No known limitations.

## Notes

- EPUB documents are essentially ZIP archives containing XHTML content files, CSS, images, and metadata. The filter processes the translatable text content within these files.
- Since no parameter editor is available, any custom configuration must be done by directly editing the configuration file in a text editor.
