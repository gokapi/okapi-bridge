# JSON Filter

The JSON Filter processes JSON (JavaScript Object Notation) files for translation. It extracts string values — both standalone and key-associated — as translatable content, with flexible control over which paths to extract using regex rules. The filter supports multiple Unicode encodings with BOM detection, inline code patterns for placeholders, sub-filtering for embedded content (e.g., HTML within JSON values), and metadata extraction rules for notes, IDs, and length constraints.

## Parameters

### Extraction

#### Extract All
Controls the default extraction behavior for key/value pairs. When **true**, all key/value string pairs are extracted, except those matching `excludeKeys`. When **false**, no key/value pairs are extracted unless they match `pathRules`.

#### Path Rules
Regex matching full JSON key paths of values to extract. **Overrides extractAll when specified.** Paths use `/`-separated key names.

Introduced in version **M39**. These rules take precedence over the older extractAll + excludeKeys approach.

Examples:
- `/widgets/body.*` — extract all keys under `/widgets/body`
- `/messages/.*|/labels/.*` — extract from multiple subtrees

#### Include Isolated Strings
Extracts string values that appear in arrays without an associated key. For example, in `[\"Hello\", \"World\"]`, the strings have no key and would not normally be extracted.

#### Exclude Keys
Keys whose string values should be excluded from extraction (when extractAll is true) or included (when extractAll is false — inverted behavior).

When `useFullPath` is enabled, regex patterns apply to full paths like `^.*?/excludedStructure/.*`.

#### Sub-filter Rules
Specify an Okapi filter ID (e.g. `okf_html`) to process the content of all translatable text values with a sub-filter.

> **Note:** Cannot be used together with inline codes. If sub-filtering is enabled, inline code detection is ignored.

### Keys

#### Use as Name
Uses the JSON key as the `resname` (resource name) for the extracted text unit.

#### Use Full Path
Uses the complete path from root to the key (e.g. `/menu/popup/menuitem/value`) instead of just the immediate key name. Requires `useAsName` to be enabled.

#### Leading Slash
Controls whether the full key path starts with a `/` character. Only applies when `useFullPath` is enabled.

#### Use ID Stack
Builds text unit IDs by stacking nested JSON keys.

#### ID Rules
Regex matching keys whose **values** become the `resname` (Block ID) of sibling text units. Overrides `useAsName`.

Introduced in version **M39**.

Example: Given `{\"key\": \"datePicker_march\", \"text\": \"March\"}`, setting idRules to `key` produces `resname=\"datePicker_march\"` for the \"March\" text unit.

### Metadata

#### Note Rules
Regex matching keys whose values are attached as translator notes (`<note>` in XLIFF) to sibling text units.

Introduced in version **M39**.

#### Meta Rules
Regex matching keys whose values are added as generic metadata (`<context-group>` in XLIFF) to sibling text units.

Introduced in version **M39**.

#### Max Width Rules
Regex matching keys whose numeric values define maximum width constraints for sibling text units. Only one matching key is allowed per hierarchy level. For nested arrays, each level can have its own maxwidth definition.

Introduced in version **M39**.

### Max Width Size Unit
The string value used as the `size-unit` attribute on XLIFF elements when maxwidthRules extracts length restrictions. Only meaningful when maxwidthRules is configured.

### Output

#### Escape Slashes
Controls whether forward slashes (`/`) are escaped as `\/` in the output JSON.

### Inline Codes

#### Enabled
Enables pattern-based detection of inline codes within extracted text. Cannot be used together with sub-filtering.

#### Rules
Regex patterns that identify inline codes. Default patterns detect printf-style specifiers (`%s`, `%d`), escape sequences (`\n`, `\t`), and ICU placeholders (`{0}`).

#### Merge Adjacent
Merges consecutive inline codes into a single placeholder.

#### Move Boundary Codes
Moves inline codes at segment boundaries to the skeleton (non-translatable).

## Limitations

- Comments within a JSON string are parsed as part of the string content, not as comments. A configured sub-filter will then process these as true comments.
- Inline codes and sub-filtering cannot be used simultaneously.

## Notes

- JSON files are normally in Unicode encoding, but the filter supports any encoding with BOM detection, charset declaration, or default encoding fallback.
- Line-break style of the output matches the input document.
- Though not technically legal in JSON, the filter supports `//`, `#`, `/* */`, and `<!-- -->` comments.
- The M39+ extraction rules (pathRules, noteRules, idRules, metaRules) override the corresponding older rules when specified.

## Examples

### Basic Extraction
```json
{\"menu\": {
  \"value\": \"File\",
  \"popup\": {
    \"menuitem\": [
      {\"value\": \"New\"},
      {\"value\": \"Open\"},
      {\"value\": \"Close\"}
    ]
  }
}}
```
All string values with keys are extracted by default.

### ID Rules with Sibling Keys
Input:
```json
[
  {\"key\": \"datePicker_marchMonth\", \"text\": \"March\"},
  {\"key\": \"datePicker_aprilMonth\", \"text\": \"April\"}
]
```
With `idRules: \"key\"`, produces text units with `resname=\"datePicker_marchMonth\"` and `resname=\"datePicker_aprilMonth\"`.
