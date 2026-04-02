# Remove Target Step

This step removes target-language content from text units in the pipeline. It can remove all targets from all text units, or selectively remove targets based on text unit IDs or target locales. The step operates on filter events and passes modified filter events downstream.

## Parameters

#### Text Unit IDs for Removal (`tusForTargetRemoval`)

Specify which text units should have their targets removed by listing their IDs. When left empty, **all targets on all text units** are removed.

IDs should be separated by commas with no spaces around delimiters.

> **Note:** Only used when `filterBasedOnIds` is `true`.

Examples:
- `"id1,id2,id3"` — remove targets only from these three text units
- `""` (empty) — remove targets from all text units

#### Target Locales to Keep (`targetLocalesToKeep`)

Specify which target locales should be **kept** (not removed). Targets with locales not in this list will be removed. When left empty, all targets are kept (none removed based on locale).

> **Note:** Only used when `filterBasedOnIds` is `false`.

Examples:
- `"fr-FR,es-ES"` — keep only French and Spanish targets, remove all others
- `""` (empty) — keep all targets

#### Filter by IDs (`filterBasedOnIds`)

Controls which filtering mode is active. The two modes are **mutually exclusive** — you cannot filter on both IDs and locales simultaneously.

- **true** (default): Uses `tusForTargetRemoval` to select text units by ID for target removal
- **false**: Uses `targetLocalesToKeep` to select which target locales to preserve

#### Remove Empty Text Units (`removeTUIfNoTarget`)

Controls whether a text unit should be completely removed from the output if it has no remaining targets after the removal process. When disabled (default), text units with no targets are passed through unchanged (source-only).

## Limitations

- The wiki states no known limitations.

## Notes

- All target languages are removed from matching text units — not just a specific target language.
- Filtering by IDs and filtering by locales are mutually exclusive; the `filterBasedOnIds` flag determines which mode is active.

## Examples

### Remove all targets

Remove targets from every text unit in the pipeline. Leave `tusForTargetRemoval` empty and keep `filterBasedOnIds` as `true`.

```json
{
  "tusForTargetRemoval": "",
  "filterBasedOnIds": true,
  "removeTUIfNoTarget": false
}
```

### Remove targets from specific text units

Remove targets only from text units with IDs `tu1` and `tu2`, leaving all other text units untouched.

```json
{
  "tusForTargetRemoval": "tu1,tu2",
  "filterBasedOnIds": true,
  "removeTUIfNoTarget": false
}
```

### Keep only specific locales

Keep French and German targets, removing all other target locales. Switch to locale-based filtering by setting `filterBasedOnIds` to `false`.

```json
{
  "targetLocalesToKeep": "fr-FR,de-DE",
  "filterBasedOnIds": false,
  "removeTUIfNoTarget": true
}
```
