# Moses Text Filter

The Moses Text Filter processes InlineText files used by the [Moses MT system](http://www.statmt.org/moses/). Each line in the file is read as a separate text unit. Inline codes are represented using `<g>`, `<x>`, `<bx>`, and `<ex>` elements with numeric identifiers, and line-breaks within entries are represented by `<lb/>`.

## Parameters

This filter has no parameters.

## Processing Notes

- If the document has a BOM, it is used to determine the input encoding; otherwise **UTF-8** is used regardless of any specified default encoding.
- Output encoding is always forced to **UTF-8**.
- Output line-break style matches the original input.
- Reading a Moses InlineText file may produce more text units than the original document because each segment is extracted as a single entry, and the Moses format has no way to mark that multiple entries belong to the same text unit.
- Use the **Moses InlineText Extraction Step** to create Moses files from other formats (including XLIFF), and the **Moses InlineText Leveraging Step** to re-group entries back into their original text units.

## Inline Code Format

Inline codes use the following elements:

| Element | Description |
|---------|-------------|
| `<g id="N">...</g>` | Paired inline code (e.g., bold, italic) |
| `<x id="N"/>` | Standalone inline code |
| `<bx id="N"/>` | Beginning of paired code |
| `<ex id="N"/>` | End of paired code |
| `<lb/>` | Line-break within an entry |

## Limitations

None known.

## Examples

### Moses InlineText file

```
Text in the first entry.
Text of the second entry<lb/>which spans<lb/>several lines
Third entry.
Fourth entry with <g id="1">bold words</g> and some code:<x id="2"/>
```

Each line is read as a separate text unit. The second entry spans multiple lines in the original document, joined by `<lb/>` codes.
