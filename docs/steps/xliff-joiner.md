# XLIFF Joiner Step

Re-joins multiple XLIFF documents that were previously split (e.g., by the XLIFF Splitter Step) back into one or more combined documents based on filename patterns. Files sharing the same naming pattern (differing only by an index after the marker) are grouped and merged by collecting their `<file>` elements into a single output document. The first document in each group provides the surrounding XLIFF structure, while subsequent documents contribute only their `<file>` elements.

Takes: Raw documents. Sends: Raw document.

## Parameters

#### Input File Marker

A text marker used to identify which input files belong to the same group and should be joined together. The step looks for this marker in each input filename — files whose names match the pattern `{basename}{marker}{index}.xlf` are grouped by `{basename}` and merged.

Files whose names do **not** contain this marker are skipped entirely.

Default: `_PART`

**Examples:**
- With marker `_PART`: files `File1_PART001.xlf`, `File1_PART002.xlf` are grouped as `File1`
- With marker `-chunk`: files `Doc-chunk1.xlf`, `Doc-chunk2.xlf` are grouped as `Doc`

> **Note:** Files are joined in the order they arrive in the pipeline, **not** by their index number.

#### Output File Marker

The text that replaces the input marker (and its trailing index) in the output filename. Each group of joined files produces a single output file named `{basename}{outputMarker}.xlf`.

Default: `_CONCAT`

> **Warning:** If files with the same names already exist in the output directory, they will be overridden without warning.

**Examples:**
- Input marker `_PART` + output marker `_CONCAT`: `File1_PART001.xlf` → `File1_CONCAT.xlf`
- Input marker `_PART` + output marker (empty): `File1_PART001.xlf` → `File1.xlf`

## Notes

- The first document in each naming-pattern group provides the XLIFF structure (content before the first `<file>` and after the last `<file>` element). Subsequent documents contribute only their `<file>` elements.
- Content before the first `<file>` and after the last `<file>` in subsequent documents is discarded, since the XLIFF Splitter Step duplicates this structural content into every split file.
- Files are joined in pipeline arrival order, regardless of the numeric index appended by the XLIFF Splitter Step.
- To split a single XLIFF document into several ones, use the XLIFF Splitter Step.

## Limitations

None known.

## Examples

### Rejoin split XLIFF files

With default parameters:

```yaml
inputFileMarker: _PART
outputFileMarker: _CONCAT
```

**Input files:**
```
C:\Project\FolderABC\File1_PART001.xlf
C:\Project\FolderABC\File1_PART002.xlf
C:\Project\FolderABC\File2_PART001.xlf
C:\Project\FolderABC\File2_PART002.xlf
C:\Project\FolderABC\File3.xlf
```

**Output files:**
```
C:\Project\FolderABC\File1_CONCAT.xlf   (joined from File1_PART001 + File1_PART002)
C:\Project\FolderABC\File2_CONCAT.xlf   (joined from File2_PART001 + File2_PART002)
C:\Project\FolderABC\File3.xlf          (skipped — no "_PART" marker)
```
