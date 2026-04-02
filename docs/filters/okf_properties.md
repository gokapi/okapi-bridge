# Properties Filter

The Properties Filter processes Java `.properties` files, extracting key-value pairs as translatable text units. It is based on the `java.util.Properties` specification and supports additional comment styles for compatibility with other properties-like formats. The filter supports localization directives, key-based filtering, sub-filters for embedded content (e.g., HTML), and inline code detection via regex patterns.

Java properties can also be represented in XML — use the XML Filter or XML Stream Filter for those.

## Parameters

### Extraction

#### Enable Key Condition (`useKeyCondition`)
Enables filtering of entries based on their property key. When enabled, only entries whose keys match (or don't match) the `keyCondition` regex are extracted. Localization directives take precedence over key conditions.

#### Extract Only Matching Keys (`extractOnlyMatchingKey`)
Controls the key filtering behavior:
- **true** — Extract **only** items with keys matching the pattern
- **false** — Extract all items **except** those with keys matching the pattern

Only meaningful when the key condition is enabled.

#### Key Condition Pattern (`keyCondition`)
Java regular expression tested against property keys. Uses `java.util.regex.Pattern` syntax.

Examples:
- `.*text.*` — matches keys containing "text"
- `^msg\.` — matches keys starting with "msg."

#### Extract Outside Directives (`localizeOutside`)
Controls whether items not within the scope of a localization directive are extracted. Only meaningful when localization directives are enabled.

#### Additional Comment Markers (`extraComments`)
Recognizes comment styles beyond `#` and `!`:
- Lines starting with `;`
- Lines where `//` is the first non-whitespace sequence

Note: `//` after `=` is treated as part of the value, not a comment.

### Notes

#### Comments as Notes (`commentsAreNotes`)
Attaches comments preceding each entry as a note on the corresponding text unit. All comment lines before an entry are grouped into a single note.

### Identification

#### Use Key as Text Unit ID (`idLikeResname`)
Uses the property key as the text unit ID. The key is already extracted into the `name` property (`resname` in XLIFF) regardless. Property keys may not be valid IDs for all output formats (e.g., XLIFF2 requires NMTOKEN). Available since M31.

#### Use Localization Directives (`useLd`)
Enables recognition of localization directives — special comments that override default extraction behavior. Directives override key conditions when both are active.

### Output

#### Escape Extended Characters (`escapeExtendedChars`)
Converts all characters above U+007F to `\uHHHH` escape sequences. When disabled, only characters unsupported by the output encoding are escaped.

#### Retain Java Escapes (`useJavaEscapes`)
Retains Java property escape sequences in extracted text rather than converting them to actual characters.

#### Convert \n and \t (`convertLFandTab`)
Converts `
` and `	` escape sequences to actual line-breaks and tabs. All other escaped characters remain escaped.

### Inline Codes

#### Enable Inline Codes (`inlineCodes.enabled`)
Enables pattern-based detection of inline codes. Matches are converted to placeholders protected from translation. Default patterns detect printf-style specifiers, escape sequences, and MessageFormat placeholders.

#### Code Finder Rules (`inlineCodes.rules`)
Regex patterns identifying inline codes in translatable text.

#### Merge Adjacent (`inlineCodes.mergeAdjacent`)
Merge consecutive inline codes into a single placeholder.

#### Move Boundary Codes (`inlineCodes.moveBoundaryCodes`)
Move inline codes at segment boundaries to the skeleton (non-translatable portion).

## Limitations

None known.

## Notes

- BOM handling: if the input has a Unicode BOM, the corresponding encoding is used. Output BOM is only preserved for UTF-8 → UTF-8 conversions where the input had a BOM.
- Line-breaks in the output match the original input format.
- Each property entry maps to one text unit: key → name (`resname`), value → source content, preceding comments → note (if enabled).

## Examples

### Basic properties file

```properties
# This is a comment

labelOK= OK
msgBadFile: Invalid input file
```

### Key-based filtering

Using `keyCondition: ".*text.*"` with extract-only-matching enabled:

```properties
key1 = Text for key1
text.err1 = Text for text.err1
menu_text_file = Text for menu_text_file
```

Result: only `text.err1` and `menu_text_file` are extracted.
