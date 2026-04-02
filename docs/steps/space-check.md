# Space Quality Check

Fixes spaces around target inline codes within text units so that spacing matches the source. If there is a mismatched number of inline codes between source and target, unmatched codes are skipped and the remainder of the text unit is still processed.

## Parameters

This step has no parameters.

## Limitations

None known.

## Notes

- Spaces are either inserted or removed in the target segments so that the spacing around each inline code in the source is matched.
- When inline code counts differ between source and target, unmatched codes are skipped and the rest of the text unit is still processed — no error is raised.

## Examples

#### Basic space correction around inline codes

If the source has `Hello <b>world</b>` (no space before `<b>`) but the target has `Bonjour <b>monde</b>` (extra space before `<b>`), the step removes the extra space in the target to match the source pattern.
