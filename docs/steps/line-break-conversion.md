# Line-Break Conversion Step

Converts all line-breaks in a text-based input document to a specified type (DOS/Windows, Unix/Linux, or Macintosh). The step auto-detects existing line-break types and handles mixed line-break styles within a single file. Input encoding is preserved in the output, and Byte-Order-Marks are kept if present.

Takes: Raw document. Sends: Raw document.

## Parameters

#### Convert line-breaks to the following type

Controls which line-break style is written to the output file. The input file's line-break types are auto-detected, and mixed styles are supported — all are normalized to the chosen type.

| Value | Line-Break | Hex | Escape |
|---|---|---|---|
| `$0d$$0a$` | DOS/Windows (CR+LF) | `0x0D 0x0A` | `
` |
| `$0a$` | Unix/Linux (LF) | `0x0A` | `
` |
| `$0d$` | Macintosh (CR) | `0x0D` | `` |

> **Note:** The input encoding must be specified explicitly, even for formats that usually auto-detect encoding (XML, HTML, etc.). The only exception is files with a Byte-Order-Mark.

> **Note:** The output encoding always matches the input encoding — no transcoding occurs. BOMs are preserved if present.

## Limitations

- None known.

## Notes

- Input encoding must be specified explicitly for all formats except BOM-prefixed files.
- Output encoding always matches input encoding — no transcoding is performed.
- Byte-Order-Marks are preserved if present in the input, but never added or removed.
- Mixed line-break types within a single input file are handled correctly — all are converted to the target type.

## Examples

### Convert Windows line endings to Unix

Normalize a Windows-formatted file to Unix line endings:

```yaml
lineBreak: "$0a$"
```
