# XML Characters Fixing

Replaces characters that are invalid in XML documents with a configurable marker string. The step processes raw documents, recognizing both raw invalid characters and numeric character references (NCRs) such as `&#000B;` or `&#11;`. Output preserves the input encoding, and Byte-Order-Marks are maintained if present.

## Parameters

#### Replacement String

The replacement string uses [Java Formatter syntax](http://download.oracle.com/javase/6/docs/api/java/util/Formatter.html). The parameter passed to the formatter is always the **integer value** of the Unicode code-point of the invalid character.

To **remove** invalid characters entirely, leave this field empty.

Special characters like `%` must be escaped (e.g., `%%` to produce a literal `%`).

**Examples:**

| Invalid Character | Replacement String | Result |
|---|---|---|
| U+000B | `x%03X` | x0B |
| U+000B | `_#x%X;` | _#xB; |
| U+000B | `?` | ? |
| U+000B | `&#xFFFD;` | &#xFFFD; |
| U+000B | `%%%d%%` | %11% |

## Notes

- Input encoding must be specified unless the document has a Byte-Order-Mark (BOM).
- Output encoding matches the input encoding; BOMs are preserved if present.
- The step recognizes both raw invalid characters and numeric character references (NCRs) like `&#000B;` or `&#11;`.

## Limitations

None known.
