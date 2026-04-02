# Inline Codes Removal Step

Removes inline codes or content of inline codes from text units in a document. Inline codes are spans of data that should normally be left untouched inside translatable text (e.g., `<b>` tags in HTML). The step offers three removal modes: removing just the markers, just the content, or both. It operates on filter events and can selectively target source text, target text, and non-translatable units.

## Parameters

#### What to remove (`mode`)

An inline code has two parts: **markers** (always present) and **content** (optional).

For example, in XLIFF: `<bpt id="1">&lt;b&gt;</bpt>` — the `<bpt>` / `</bpt>` tags are the **marker**, and `&lt;b&gt;` is the **content**.

Available modes:
- **Remove code marker, but keep code content** (0) — The content becomes part of the regular text (e.g., the literal `<b>` appears as text).
- **Remove code content, but keep code marker** (1) — The inline code still exists structurally but is generalized; original format data cannot be recovered.
- **Remove code marker and code content** (2) — The entire inline code is removed with no trace left.

Default: 2 (remove both).

#### Strip codes in the source text (`stripSource`)

Applies the selected removal action to the **source** text of each text unit. When disabled, source inline codes are left intact regardless of the removal mode chosen.

Default: true.

#### Strip codes in the target text (`stripTarget`)

Applies the selected removal action to the **target** text of each text unit. When disabled, target inline codes are left intact regardless of the removal mode chosen.

Default: true.

#### Apply to non-translatable text units (`includeNonTranslatable`)

Controls whether the removal action is also applied to text units marked as **non-translatable**. When disabled, all non-translatable text units are left completely untouched.

Default: true.

#### Replace line break codes with spaces (`replaceWithSpace`)

Replaces line-break inline codes with a space character instead of simply removing them. A code is treated as a line break if its type is `Code.TYPE_LB` or its content contains any of the following (case-insensitive):

- `<br>`, `<br />`, `<br/>`
- `
`, ``
- `\u0085`, `\u2028`, `\u2029`

This prevents words on adjacent lines from being concatenated when codes are stripped.

> **Note:** This option only applies when the **Remove code marker and code content** mode is selected (mode = 2).

Default: false.

## Limitations

- None known.

## Notes

- The step operates on filter events — it must be placed after a filter in the pipeline.
- Both source and target stripping are enabled by default, so the step processes both sides unless explicitly disabled.

## Examples

### Strip all inline codes from XLIFF

Removes both markers and content from all text units (default configuration):

```yaml
stripSource: true
stripTarget: true
mode: 2
includeNonTranslatable: true
replaceWithSpace: false
```

Input:
```xml
<source>Click <bpt id="1">&lt;b&gt;</bpt>here<ept id="1">&lt;/b&gt;</ept> to continue</source>
```

Output:
```xml
<source>Click here to continue</source>
```

### Replace line-break codes with spaces

When removing all code parts, line-break codes (e.g., `<br>` tags) are replaced with spaces to prevent words from merging:

```yaml
mode: 2
replaceWithSpace: true
stripSource: true
stripTarget: true
```
