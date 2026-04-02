# RTF Conversion Step

Removes the RTF layer from text-based input files, outputting only the visible text content. Formatting other than spaces, line-breaks, and tabs is discarded, along with hidden/deleted text, images, and special content. Can be used to post-process Trados-Tagged RTF files, extracting only the translated sections.

Takes: Raw document. Sends: Raw document.

## Parameters

#### Use Byte-Order-Mark for UTF-8 output

Adds a Byte-Order-Mark (BOM) at the beginning of the output file when the output encoding is UTF-8. Has no effect for non-UTF-8 output encodings.

See the [Unicode BOM FAQ](http://www.unicode.org/faq/utf_bom.html) for details on when a BOM is appropriate.

> **Note:** Only applies when the output encoding is UTF-8 — ignored for other encodings.

#### Try to update the encoding declarations

Automatically updates encoding declarations in the output file to match the selected output encoding. Supports two file types:

- **XML files**: Updates the `encoding="..."` attribute in the XML declaration. If the declaration exists but has no encoding attribute, one is added. If there is no XML declaration at all, nothing is updated.
- **HTML files**: Looks for the pattern `content=... charset=...` and updates the `charset` value. If the pattern is not found, nothing is updated.

> **Warning:** The encoding update uses pattern matching, not full parsing. A commented-out declaration could be incorrectly updated.

#### Type of line-break to use

Controls which line-break sequence is written to the output file:

- **DOS/Windows** — `
` (CR+LF, 0x0D+0x0A)
- **Unix/Linux** — `
` (LF, 0x0A)
- **Macintosh** — `` (CR, 0x0D)

## Limitations

- Characters encoded as `SYMBOL` fields (e.g., drawing symbols from Word using Dingbats or Wingdings fonts) are not output.
- Encoding declaration updates for XML and HTML use pattern matching rather than parsing, so commented-out declarations may be incorrectly updated.
- All formatting beyond spaces, line-breaks, and tabs is discarded.
- Hidden text, deleted text, images, and other special content are discarded.

## Notes

- A warning is issued for each line where one or more characters cannot be represented in the selected output encoding.
- Can be used to post-process Trados-Tagged RTF files — only the translated sections are output.
- For access to both source and target text in Trados-Tagged RTF files, use the Trados-Tagged RTF Filter instead.

## Examples

### Post-process Trados-Tagged RTF

Use the RTF Conversion step after reading a Trados-Tagged RTF file to extract only the translated text. The step strips the RTF layer and outputs plain text with only the translated sections. For full source+target access with inline codes, use the Trados-Tagged RTF Filter instead.
