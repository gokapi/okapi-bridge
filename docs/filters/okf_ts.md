# TS Filter

The TS Filter processes **Qt TS (Translation Source)** resource files used in Qt Linguist localization workflows. It implements the `IFilter` interface in the class `net.sf.okapi.filters.ts.TsFilter`. The implementation follows the [TS File Format specification](http://doc.trolltech.com/4.3/linguist-ts-file-format.html). The filter handles translation state mapping from TS `type` attributes and supports `<byte/>` element conversion.

## Parameters

### Options

#### Convert Byte Elements

Controls how `<byte value="[value]"/>` elements in the TS document are handled.

- **Enabled**: byte elements are converted to their raw character equivalents
- **Disabled**: byte elements are preserved as inline codes

These characters are stored as special elements in TS because they are **invalid in XML**. If your downstream format is XML-based (XLIFF, TMX), it is generally safer to keep them as inline codes.

> **Note:** Converting byte elements to raw characters may cause issues when working with XML-based exchange formats like XLIFF or TMX, since those characters are invalid in XML.

### Inline Codes

#### Has Inline Codes

Enables pattern-based inline code detection using regular expressions. Any text matching a rule is converted to an inline code rather than translatable text.

The default pattern matches:
- **printf-style placeholders**: `%d`, `%s`, `%2$s`, `%-10.2f`, etc.
- **Escape sequences**: `
`, ``, `	`, ``, ``, ``, ``, `
`
- **Numbered placeholders**: `{0}`, `{1}`, `{2 something}`, etc.

Default expression:
```regex
((%(([-0+#]?)[-0+#]?)((\d\$)?)(([\d\*]*)(\.[\d\*]*)?)[dioxXucsfeEgGpn])
|((\r\n)|\a|\b|\f|\n|\r|\t|\v)
|(\{\d.*?\}))
```

Each rule must be a valid Java regular expression (`java.util.regex.Pattern` syntax).

## Processing Details

### Translation State Mapping

The `type` attribute on `<translation>` elements maps to Okapi properties:

| TS `type` attribute | Okapi behavior |
|---|---|
| *(none)* | Extracted, approved = `yes` |
| `unfinished` | Extracted, approved = `no` |
| `obsolete` | **Not extracted** (treated as unused entries) |

### Translator Comments

The `<translatorcomment>` element is stored as text unit `Property.TRANSNOTE` and can be used by subsequent pipeline steps or editors.

### Line-Breaks

The type of line-breaks in the output matches the original input file.

## Limitations

- There are sometimes issues with extracting and merging back `<numerusform>` elements. Perform a round-trip test before sending for translation.

## Examples

### Translation State Mapping

Shows how the TS `type` attribute on `<translation>` maps to Okapi's `approved` property:

| TS `type` attribute | Okapi behavior |
|---|---|
| *(none)* | Extracted, approved = `yes` |
| `unfinished` | Extracted, approved = `no` |
| `obsolete` | **Not extracted** |

### Default Inline Code Pattern

The default regular expression detects printf placeholders, escape sequences, and numbered placeholders:

Input:
```
File %s has %d errors
See {0} for details
```

Output (inline codes shown in angle brackets):
```
File <1> has <2> errors<3>See <4> for details
```
