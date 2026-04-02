# Full-Width Conversion Step

Converts characters in text units between full-width (zenkaku) and half-width (hankaku) forms, targeting Asian character sets that historically have dual display forms. The conversion operates on the target locale text of text units; if no target text exists, source text is copied to the target before processing. Supports both directions: full-width → half-width/ASCII and half-width/ASCII → full-width, with fine-grained control over which character classes are affected.

**Takes:** Filter events. **Sends:** Filter events.

## Parameters

### Conversion Direction

#### Convert to Half-Width

Controls the conversion direction.

- **true** (default): Converts full-width characters to half-width or ASCII equivalents. E.g., `Ｑ` (U+FF31) → `Q` (U+0051), `サ` (U+30B5) → `ｻ` (U+FF7B).
- **false**: Converts half-width and ASCII characters to full-width equivalents. E.g., `Q` (U+0051) → `Ｑ` (U+FF31), `ｻ` (U+FF7B) → `サ` (U+30B5).

### Half-Width Options (toHalfWidth = true)

#### Include Squared Latin Abbreviations

Also converts Squared Latin Abbreviations from the CJK Compatibility block (U+3300–U+33FF) into sequences of non-CJK characters. Example: `㏀` (U+33C0) → `kΩ`.

#### Include Letter-Like Symbols

Also converts several characters from the Letter-Like Symbols block to character sequences:

| Symbol | Result |
|--------|--------|
| U+2100 | `a/c` |
| U+2101 | `a/s` |
| U+2105 | `c/o` |
| U+2103 | `°C` |
| U+2109 | `°F` |
| U+2116 | `No` |
| U+212A | `K` |
| U+212B | `Å` |

#### Include Katakana

Also converts Japanese Katakana and associated punctuation (`。、「」`, etc.) into their half-width forms. Off by default because in modern Japanese text normalization, Katakana is typically kept full-width while only alphanumeric characters are converted.

> Available since 0.29-SNAPSHOT.

### Full-Width Options (toHalfWidth = false)

#### ASCII Only

Restricts conversion to ASCII characters only. Half-width Katakana and other half-width characters are left unchanged.

#### Katakana Only

Restricts conversion to Japanese Katakana and associated punctuation (`｡､｢｣`, etc.) only. Alphanumeric characters remain half-width.

> Available since 0.29-SNAPSHOT.

### Output

#### Normalize Output (NFC)

Applies Unicode NFC normalization to the output text after conversions. Converting half-width Katakana to full-width can produce decomposed character sequences — for example, `ﾌﾟ` (U+FF8C U+FF9F) → `プ` (U+30D5 U+309A, decomposed). NFC normalization produces the standard precomposed form `プ` (U+30D7). Enabled by default.

> Available since 0.29-SNAPSHOT.

## Limitations

None known.

## Notes

- Operates on the target locale text of text units. If no target text exists, the source text is copied to the target before processing.
- The step handles two distinct conversion directions controlled by `toHalfWidth`, each with its own set of sub-options: `includeSLA`/`includeLLS`/`includeKatakana` apply to half-width conversion, while `asciiOnly`/`katakanaOnly` apply to full-width conversion.

## Examples

### Normalize Japanese full-width alphanumerics to half-width

Convert full-width ASCII-range characters to standard half-width while keeping Katakana full-width:

```yaml
toHalfWidth: true
includeKatakana: false
normalizeOutput: true
```

Input: `Ｈｅｌｌｏ サンプル １２３` → Output: `Hello サンプル 123`

### Convert ASCII to full-width for CJK typesetting

```yaml
toHalfWidth: false
asciiOnly: true
```

Input: `Hello 123` → Output: `Ｈｅｌｌｏ １２３`

### Normalize half-width Katakana to full-width

```yaml
toHalfWidth: false
katakanaOnly: true
normalizeOutput: true
```

Input: `Hello ｻﾝﾌﾟﾙ 123` → Output: `Hello サンプル 123`
