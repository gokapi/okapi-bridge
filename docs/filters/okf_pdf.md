# PDF Filter

The PDF Filter extracts text content from PDF files for processing. It does not handle complex formatting like tables or multi-level lists. The typical use case is scraping text from PDFs for quick word counts and leverage analysis. **Important:** This filter does not merge back into PDF format — it produces plain text output upon merging.

## Parameters

This filter has no parameters.

## Processing Details

### Input Encoding

PDF files are binary and do not have a specific encoding. Okapi extracts all text as a Java string and forces the encoding to `UTF-16`. Any encoding selected in tools like Rainbow will be ignored.

### Segmentation

TextUnits are created following the default rules of the Plain Text filter. Any text followed by a newline creates a new TextUnit (paragraph).

## Limitations

- This filter merges back in **plain text format**, not PDF.
- Does not handle complex formatting like tables or multi-level lists.
- Any encoding selected in tools like Rainbow will be ignored — the filter forces UTF-16.

## Notes

- The primary use case is text extraction for word counts and leverage analysis, not round-trip translation back to PDF.
