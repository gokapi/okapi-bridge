# Id-Based Copy Step

Copies text from a reference file (second input) into a destination file (first input) by matching text unit ids (`TextUnit.getName()`). When a match is found, monolingual reference sources are copied to the destination's target; for multilingual references, the target text is used. Unmatched entries in the destination file remain untouched.

Works with any filter that produces unique names for text units, such as the Properties Filter.

## How It Works

For each pair of input files:

1. The **reference file** (second input) is read into a table indexed on the name of each text unit.
2. The **destination file** (first input) is processed — for each text unit:
   - If a match is found in the reference table: the source (monolingual) or target (multilingual) text is copied into the destination's target.
   - If no match is found: the text unit remains untouched.
3. After processing, unused reference entries are listed as warnings.

## Parameters

#### Set the text unit as non-translatable

When a text unit in the destination file matches an entry in the reference file, this marks the text unit as **non-translatable** after copying. Entries without a match retain their original state.

> Note: Text units already marked as non-translatable in the destination file are skipped entirely.

#### Set the target property 'approved' to 'yes'

When a text unit in the destination file matches an entry in the reference file, this sets the target's `approved` property to `yes`. Useful when the reference file represents reviewed translations. Entries without a match retain their original `approved` state.

## Limitations

- Uses an in-memory table for the reference file — may hit memory limits with very large reference files.
- No adjustment is made for inline codes when copying between different file formats.
- Copies complete text unit content with no adjustment for segmented entries. Use the Desegmentation Step beforehand if needed.
- No warning is generated when a destination entry has no match in the reference file.

## Notes

- For **monolingual** references (e.g., properties), the **source** text is copied. For **multilingual** references (e.g., PO), the **target** text is used.
- Text units marked as non-translatable in the destination are never modified.
- Both files must have text units with **unique** names (e.g., unique `resname` in XLIFF).
- Entries in the reference and destination files do not need to be sorted.
- Unused reference entries generate warnings, but these may simply indicate obsolete entries.
- If a destination file has no associated reference file, a warning is generated and the file is treated as having an empty reference.

## Examples

### Aligning properties files

Copy translations from a reference properties file into a destination, marking matched entries as approved:

```yaml
markAsTranslateNo: false
markAsApproved: true
```

Both files must have matching keys (text unit names). The reference file is the second input and the destination is the first input.
