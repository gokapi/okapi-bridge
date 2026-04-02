# Encoding Conversion Step

Converts the character set encoding of an input document and optionally updates encoding declarations in XML and HTML files. The step can un-escape various character reference notations (NCR, CER, Java-style) on input, and re-escape characters using configurable notation on output. It automatically detects and updates XML encoding declarations and HTML charset declarations in the first 1024 characters of each document.

**Takes:** Raw document. **Sends:** Raw document.

## Encoding Declaration Updates

The step scans the first 1024 characters of each document to detect and update encoding declarations. Note that this scan is not XML/HTML-aware and cannot distinguish between declarations inside comments and real ones.

### XML Documents

- If an XML encoding declaration is found → the encoding value is updated
- If an XML declaration exists without encoding → an encoding declaration is inserted
- If the file has a `.xml` extension but no XML declaration → one is added at the top

### HTML Documents

- If an HTML charset declaration is found → the charset value is updated
- If the file extension starts with `.htm` and no charset exists:
  - If `<head>` is found → a charset declaration is added after it
  - Otherwise if `<html` is found → `<head>` and charset are added after the opening tag

A document can be both XML and HTML and have both types of declarations.

## Parameters

### Input

#### Unescape Numeric Character References

Un-escapes all types of numeric character references (NCRs) when reading the input. Converts decimal (`&#255;`), uppercase hex (`&#xE1;`), and lowercase hex (`&#e1;`) forms to their actual characters. If disabled, NCRs pass through unchanged.

#### Unescape Character Entity References

Un-escapes standard HTML character entity references (CERs) when reading the input (e.g., `&aacute;` → `á`).

> **Note:** The five XML-reserved entities (`&amp;`, `&lt;`, `&gt;`, `&apos;`, `&quot;`) are never un-escaped, as they may need to be preserved regardless of encoding.

#### Unescape Java-style Notation

Un-escapes Java-style `\uXXXX` escape sequences when reading the input. Both uppercase and lowercase hex digits are recognized (e.g., `\u00e1` and `\u00E1` both become `á`).

### Output

#### Escape All Extended Characters

When enabled, all extended (non-ASCII) characters are escaped in the output. When disabled, only characters unsupported by the output encoding are escaped.

#### Escape Notation

Selects the notation format for escaping characters:

| Value | Notation | Example (`á`) |
|-------|----------|---------------|
| 0 | Uppercase hex NCR | `&#xE1;` |
| 1 | Lowercase hex NCR | `&#xe1;` |
| 2 | Decimal NCR | `&#255;` |
| 3 | Character entity reference | `&aacute;` |
| 4 | Uppercase Java-style | `\u00E1` |
| 5 | Lowercase Java-style | `\u00e1` |
| 6 | User-defined notation | Uses User-Defined Format |

When using CER (3), characters without a defined entity fall back to uppercase hex NCR.

#### User-Defined Format

A Java `String.format()` expression with one integer placeholder, used when Escape Notation is set to 6. The integer is the Unicode code point (or byte value if Use Byte Values is enabled). Examples: `[[%d]]` produces `[[255]]` for `ÿ`; `\'%x` produces `\'65e5\'672c\'8a9e` for `日本語`.

#### Use Byte Values

Changes the values passed to the user-defined format expression from Unicode code points to byte values of the output encoding. Useful for multi-byte encodings like Shift-JIS where individual bytes need to be escaped separately.

#### BOM on UTF-8 Output

Adds a Byte-Order-Mark (U+FEFF) at the beginning of UTF-8 output files. Only relevant when the output encoding is UTF-8. Some applications expect a BOM to identify UTF-8 files, while others may display it as an unwanted character.

#### Report Unsupported Characters

Logs all characters not supported by the output encoding to the pipeline log, helping identify potential data loss.

## Limitations

- Encoding declaration detection operates on raw text (first 1024 chars) and is not XML/HTML-aware — it cannot distinguish declarations inside comments from real ones.
- HTML charset insertion requires a `<head>` element or at minimum an `<html` string to anchor the insertion point.
- The five XML-reserved character entities (`&amp;`, `&lt;`, `&gt;`, `&apos;`, `&quot;`) are never un-escaped by the CER option.

## Notes

- A document can be both XML and HTML and have both types of encoding/charset declarations updated.
- The step processes raw documents — it operates before or after filter-based extraction, not on segmented content.

## Examples

### User-defined escape for Shift-JIS

```yaml
escapeNotation: 6
userFormat: "\'%x"
useBytes: true
# Output encoding: Shift-JIS
```

Input: `日本語` → Output: `\'93\'fa\'96\'7b\'8c\'ea` (Shift-JIS byte values)

### Bracketed decimal format

```yaml
escapeNotation: 6
userFormat: "[[%d]]"
useBytes: false
```

Input: `á` → Output: `[[225]]` (Unicode code point in decimal)
