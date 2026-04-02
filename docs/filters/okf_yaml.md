# YAML Filter

The YAML Filter processes [YAML files](http://www.yaml.org/) and supports [Ruby on Rails](http://rubyonrails.org/) message variables. It extracts translatable string values from YAML key-value pairs, lists, and maps, assigning each entry a name composed of its full parent key path (e.g., `fr/activerecord/errors/messages/exclusion`). The filter handles Unicode BOM detection for encoding and preserves original line-break types in output.

## Parameters

### Extraction

#### Extract All Key/String Pairs
Controls the default extraction behavior for key/string pairs.

- **When enabled**: all key/string pairs are extracted, *except* those matching the key exception pattern.
- **When disabled**: no key/string pairs are extracted, *except* those matching the key exception pattern (inverted logic).

This works as a toggle that flips the meaning of the exception regex.

#### Extract Isolated Strings
Extracts strings that are **not associated with a key**, such as bare string values in YAML arrays or lists.

For example, in:
```yaml
list: [one, two, three]
```
the values `one`, `two`, `three` are isolated strings.

#### Key Exception Pattern
A regular expression matched against YAML keys. Matching keys behave **opposite** to the default extraction mode:

- If extractAll is **enabled**: matching keys are **excluded** from extraction.
- If extractAll is **disabled**: matching keys are **included** for extraction.

When full key path is enabled, the regex is matched against the full path (e.g., `menu/value/popup/menuitem/value`).

### Keys

#### Use Key as Name
Uses the YAML key as the `resname` (resource name) of each extracted text unit. This maps to the `name` attribute in XLIFF output.

#### Use Full Key Path
Uses the complete nested key path as the resource name instead of just the immediate key.

For example: `fr/activerecord/errors/messages/exclusion` instead of just `exclusion`.

> **Note:** Requires "Use Key as Name" to be enabled.

### Output

#### Escape Non-ASCII
Escapes non-ASCII characters in the output using Unicode escape sequences.

#### Line Wrapping
Controls whether long lines are wrapped in the output YAML file.

> **Warning:** Wrapped lines in the original source will be unwrapped in the target document regardless of this setting.

#### Sub-filter Literal Blocks
When sub-filtering is active, controls whether YAML literal block scalars (using `|` syntax) are processed through the sub-filter.

### Inline Codes

#### Enable Inline Code Detection
Activates pattern-based detection of inline codes within extracted text. Matched patterns are converted to inline code placeholders.

> **Note:** Cannot be used together with sub-filtering.

#### Code Finder Rules
Regular expression patterns that identify inline codes. Default patterns detect printf-style specifiers, escape sequences, and numbered placeholders.

Presets available:
- **Ruby i18n**: `%\{[^}]+\}` — matches `%{name}`, `%{count}`
- **HTML**: `<[^>]+>` — matches `<b>`, `</a>`

#### Merge Adjacent Codes
Merges consecutive inline codes with no translatable text between them into a single placeholder.

#### Move Boundary Codes
Moves inline codes at the start or end of a segment into the skeleton (non-translatable content).

#### Simplifier Rules
Rules for simplifying inline code representation for translators.

## Limitations

- Wrapped lines in the original source document will be **unwrapped** in the target document — all text appears on a single line.
- If an illegal character is introduced during translation, the user must manually add quotes to keep the YAML valid. A WARNING is issued when detected.
- Inline code detection cannot be used together with sub-filtering — they are mutually exclusive.

## Notes

- **Encoding**: Unicode BOM is detected automatically. Otherwise the default encoding specified when opening the document is used.
- **Line breaks**: Output preserves the original line-break type (CRLF, LF).
- **Entry naming**: Each entry is named by joining all parent keys with `/` (e.g., `fr/activerecord/errors/messages/exclusion`).

## Examples

### Ruby on Rails i18n file

```yaml
fr:
  activerecord:
    errors:
      messages:
        inclusion: "n'est pas inclus(e) dans la liste"
        exclusion: "n'est pas disponible"
        invalid: "n'est pas valide"
```

All string values are extracted with full key paths as names (e.g., `fr/activerecord/errors/messages/exclusion`).

### Inline code detection for Ruby interpolation

```yaml
inlineCodes:
  enabled: true
  rules:
    rules:
      - pattern: "%\{[^}]+\}"
    sample: "Hello %{name}, you have %{count} messages"
```

This protects `%{name}` and `%{count}` placeholders from translation.
