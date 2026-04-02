# Text Modification Step

Modifies the content of text units by applying various transformations such as character replacement, pseudo-localization, prefix/suffix addition, and text expansion. Text units marked as non-translatable are skipped. Useful for pseudo-localization testing, generating placeholder translations, and verifying that UI layouts can accommodate longer strings.

Takes: Filter events. Sends: Filter events.

## Parameters

#### Type of Change

Controls which text transformation is applied to each text unit's content:

- **0** — Keep the original text (no modification to content itself; prefix/suffix/name/ID options still apply)
- **1** — Replace letters with `X` and digits with `N` (masks content while preserving length)
- **2** — Remove all text but keep inline codes intact
- **3** — Replace ASCII characters with Extended Latin characters
- **4** — Replace ASCII characters with Cyrillic characters
- **5** — Replace ASCII characters with Arabic characters
- **6** — Replace ASCII characters with Chinese characters

> **Note:** Character substitution (types 3–6) does not perform translation, transliteration, or any meaningful linguistic operation — it simply swaps characters for visual testing purposes.

#### Add Prefix

Add a prefix string at the start of each text unit. Uses the **Prefix Text** value.

#### Prefix Text

The text to prepend to each text unit when prefix addition is enabled. Default: `{START_`.

#### Add Suffix

Add a suffix string at the end of each text unit. Uses the **Suffix Text** value.

#### Suffix Text

The text to append to each text unit when suffix addition is enabled. Default: `_END}`.

#### Modify Existing Translations

By default, text units that already have a translation are left unchanged. Enable this to apply modifications even when a target translation exists (e.g., in multilingual file formats like XLIFF or PO files with existing translations).

#### Append Item Name

Appends the name (resource key) of each text unit at the end of its value. If the text unit has no name, the extraction ID is appended instead.

#### Append Extraction ID

Appends the extraction ID of each text unit at the end of its value. Extraction IDs are filter-specific.

#### Mark Segment Boundaries

Wraps each segment within a text unit with `[` and `]` delimiters. Delimiters are placed after any prefix and before any appended name, ID, or suffix. If the text unit is not segmented, the delimiters wrap the full content.

#### Modify Blank Entries

Controls whether modifications are applied to text units with no actual text content — entries that are empty or contain only whitespace and/or inline codes. Enabled by default.

#### Expand Text

Expands text to simulate translation length growth for UI testing:

- Content ≤ 30 characters: expanded by 50% (minimum 1 character added)
- Content > 30 characters: expanded by 100%
- Empty strings are not expanded

#### Replacement Script

Internal parameter tracking which script variant is selected for character replacement modes. Corresponds to types 3–6.

## Limitations

None known.

## Notes

- Text units marked as non-translatable are always skipped.
- Prefix is added before segment markers; suffix, name, and ID are added after segment markers.
- Character substitution modes produce visually distinct but linguistically meaningless output.

## Examples

### Pseudo-localization with prefix/suffix

Wraps all text units with bracket markers and replaces ASCII with Extended Latin characters:

```yaml
type: 3
addPrefix: true
prefix: "[!!"
addSuffix: true
suffix: "!!]"
expand: true
applyToBlankEntries: false
```

Input: `Save` → Output: `[!!Šàvé Šàv!!]`

### Mask content with Xs

Replaces all letters with X and digits with N:

```yaml
type: 1
```

Input: `Total: 42 items` → Output: `XXXXX: NN XXXXX`
