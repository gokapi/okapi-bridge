# BOM Conversion

Adds or removes the Unicode Byte-Order-Mark (BOM) to or from UTF-8 and UTF-16 input files. The BOM is a special Unicode mark indicating byte order: little-endian (U+FFFE) or big-endian (U+FEFF). For more information, see the [Unicode BOM FAQ](http://www.unicode.org/faq/utf_bom.html).

Takes: Raw document. Sends: Raw document.

## Parameters

#### Remove BOM If Present

Controls whether the step **removes** or **adds** the BOM:

- **Enabled**: Removes the BOM from input files if one is detected. By default, only UTF-8 BOMs are removed.
- **Disabled**: Adds a BOM to input files if one is not already present. The input files must already be in UTF-8 or UTF-16, and you must specify the encoding of each file so the correct type of BOM is added.

> **Note:** When adding a BOM, you must specify the encoding of each file so the utility can add the proper type of BOM.

#### Remove UTF-16 BOMs Also

Extends BOM removal to also cover UTF-16 files. By default, only UTF-8 BOMs are removed when removal is enabled.

Only meaningful when **Remove BOM If Present** is enabled.

> **Warning:** Removing the BOM from UTF-16 files is not recommended. UTF-16 files must have a BOM to indicate byte order.

## Limitations

- Only UTF-8 and UTF-16 files are supported — UTF-32 is not currently handled.

## Notes

- The step operates on raw documents — it does not parse or segment content.
- When adding BOMs, the correct BOM type (UTF-8 vs UTF-16 LE/BE) is determined from the file's encoding setting.

## Examples

### Remove UTF-8 BOM

Remove the BOM from UTF-8 files only (the default removal behavior):

```yaml
removeBOM: true
alsoNonUTF8: false
```

### Add BOM to files missing one

Add a BOM to UTF-8 or UTF-16 input files that do not already have one. Ensure the encoding is specified for each file:

```yaml
removeBOM: false
```
