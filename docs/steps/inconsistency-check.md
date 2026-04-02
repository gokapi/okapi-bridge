# Inconsistency Check

Generates a report of potential inconsistencies found by comparing source and target segments across multiple input documents. It checks for cases where the same source text has different translations, and where the same target text corresponds to different sources. The XML report groups findings by "key text" for easy review. Comparisons are case-sensitive, whitespace-sensitive, and ignore inline codes.

## Parameters

#### Check Inconsistencies
Master toggle for the inconsistency checking functionality. Disabling this effectively turns the step into a pass-through, allowing you to disable it in a pipeline without removing it.

> **Note:** This is a convenience toggle to disable the step within a pipeline configuration without removing it entirely.

#### Per-File Checking
Processes each input file individually rather than comparing segments across all files.

By default, segments are compared across **all** input documents, which requires keeping all segments in memory. Enabling this option limits comparison scope to a single file at a time, significantly reducing memory usage at the cost of not detecting cross-file inconsistencies.

> **Note:** Consider enabling this option if you experience memory issues with large document sets.

#### Report File Path
Full path for the generated XML inconsistency report.

Supported variables:
- `${rootDir}` — the pipeline root directory
- `${inputRootDir}` — the input root directory

Examples:
- `${rootDir}/inconsistency-report.xml` — default, writes to the pipeline root
- `${inputRootDir}/qa/inconsistencies.xml` — writes to a qa subfolder under the input root

#### Inline Code Display
Controls how inline codes are rendered in the generated report.

- **Original codes**: Inline codes appear as they do in the source documents
- **Generic markers**: Codes are replaced with numbered markers like `<1>``</1>` or `<2/>`
- **Plain text**: All inline codes are stripped from the report output

#### Auto-Open Report
Automatically opens the generated XML report file in the system's default application after processing completes. Useful for interactive/desktop usage but should typically be disabled in automated pipelines or server environments.

## Limitations

- May run out of memory when processing a large number of segments across many files. Use the per-file option to mitigate.
- Inline codes are ignored during comparisons — differences only in inline codes will not be flagged.
- Comparisons are case-sensitive and whitespace-sensitive — `Hello` and `hello` are treated as different texts.
- Text units or segments with no target are excluded from the check.
- Empty text units or segments are included in the check.

## Notes

- The step collects all segments in memory (unless per-file mode is enabled) before generating the report, so memory usage scales with the total number of segments.
- The step is a pass-through for filter events — it reads and analyzes segments but does not modify them.
- The report is XML format, grouping inconsistencies by their "key text" with all corresponding source or target variants listed.

## Examples

### Basic inconsistency check
Runs inconsistency checking across all input documents with default settings:

```yaml
checkInconststencies: true
checkPerFile: false
outputPath: ${rootDir}/inconsistency-report.xml
displayOption: generic
autoOpen: true
```

### Large-scale batch check
For processing many files with reduced memory usage in an automated pipeline:

```yaml
checkInconststencies: true
checkPerFile: true
outputPath: ${rootDir}/qa/inconsistency-report.xml
displayOption: generic
autoOpen: false
```
