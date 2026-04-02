# TTX Joiner

Re-joins multiple TTX documents that were previously split (by the TTX Splitter Step) back into single documents. The step sorts input files by their part markers, groups them by original filename, and concatenates each group into one output TTX file placed in the same directory as the input documents.

## Parameters

#### Optional suffix

A marker text appended to the end of the output filename. The suffix distinguishes joined output files from the original split parts. Default: `_joined`.

If a file with the resulting name already exists in the output directory, it will be **overwritten without warning**.

**Examples:**
- `_CONCAT` — `index.html_part001.ttx` + `index.html_part002.ttx` → `index.html_CONCAT.ttx`
- `_joined` (default) — `someFile.html_part001.ttx` + `someFile.html_part002.ttx` → `someFile.html_joined.ttx`

> **Warning:** Existing files with the same output name will be silently overwritten — no confirmation or backup is made.

## Limitations

- This step is triggered at the **END_BATCH** event, not per-document — all input files must be available before processing begins.
- Input files with no part marker or an invalid part marker are **ignored** with a warning.
- Existing output files with the same name are overwritten without warning.

## Notes

- The step internally sorts all input files so parts are processed in the correct order, regardless of the order they appear in the input list.
- Files are grouped by their original filename (before the part marker), and each group is joined into a single output file.
- The output file is written to the same directory as the input documents.
- The received pipeline event is passed through unaltered.
- To split a single TTX document into several parts, use the **TTX Splitter Step**.

## Examples

### Joining split TTX files with a custom suffix

Given four TTX parts from two original files, the joiner sorts them, groups by original filename, and produces two joined output files with the `_CONCAT` suffix.

**Configuration:**
```yaml
suffix: "_CONCAT"
```

**Input files:**
```
C:\Project\FolderABC\index.html_part001.ttx
C:\Project\FolderABC\someFile.html_part002.ttx
C:\Project\FolderABC\index.html_part002.ttx
C:\Project\FolderABC\someFile.html_part001.ttx
```

**Output files:**
```
C:\Project\FolderABC\index.html_CONCAT.ttx
C:\Project\FolderABC\someFile.html_CONCAT.ttx
```
