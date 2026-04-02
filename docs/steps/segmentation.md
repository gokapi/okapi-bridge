# Segmentation Step

Segments the content of extracted text units into smaller parts (typically sentences) using SRX 2.0 segmentation rules. The step processes filter events and applies regular-expression-based break/non-break patterns defined in an SRX document. Text units flagged as non-translatable, empty, or already segmented (unless overwrite is enabled) are skipped.

The separation between text units is based on the structure of the original file format (e.g., two `<p>` elements in HTML produce two text units). This step breaks down the content within those text units into segments.

## Parameters

### Source

#### Segment Source Text
Controls whether the source text of each text unit is segmented using the configured SRX rules. The SRX document path supports `${rootDir}` and `${inputRootDir}` variables.

> Text units flagged as non-translatable or with no content are never segmented, regardless of this setting.

### Target

#### Segment Target Text
Controls whether existing target text is segmented using SRX rules. Only target locales currently being processed are affected — other locales in the document remain untouched. The target SRX document can be the same as the source SRX document.

### Options

#### Overwrite Existing Segmentation
When a text unit is already segmented, re-segments it using the current SRX rules. The previous segmentation is completely replaced. Conflicts with **Deepen Existing Segmentation**.

#### Deepen Existing Segmentation
Keeps existing segmentation but attempts to split existing segments further using the current SRX rules. Creates additional segments only if the new rules provide higher granularity. Conflicts with **Overwrite Existing Segmentation**.

#### Skip Segmentation if Target Exists
Skips segmentation entirely for text units that already have target content. Useful when pre-translated content should be preserved as-is.

#### Force Segmented Output
Forces file formats that support segmentation representation to show segments in the output, overriding the filter's own configuration. For example, the XLIFF Filter normally only outputs segments for entries that had segments in the input — this option forces segments to be shown for all text units.

#### Verify Segment Alignment
Verifies that each source segment has a corresponding target segment when target content exists. Checks segment count and ID matching but does **not** verify translation correctness.

#### Renumber Code IDs
Reassigns inline code IDs within each segment so they start at `1`. Useful with translation memories that expect sequential IDs per segment. Use the **Restore original IDs** option in the Desegmentation Step to reverse this.

> **Warning:** Cannot work with documents where code ID values are not numeric or not sequential.

#### Trim Source Leading Whitespace
Maximum leading whitespace characters to trim from source segments. Use `-1` for default behavior, `0` to disable trimming.

#### Trim Source Trailing Whitespace
Maximum trailing whitespace characters to trim from source segments. Use `-1` for default behavior, `0` to disable trimming.

#### Trim Target Leading Whitespace
Maximum leading whitespace characters to trim from target segments. Use `-1` for default behavior, `0` to disable trimming.

#### Trim Target Trailing Whitespace
Maximum trailing whitespace characters to trim from target segments. Use `-1` for default behavior, `0` to disable trimming.

#### Treat Isolated Codes as Whitespace
Treats isolated inline codes (not part of a paired open/close sequence) as whitespace for segmentation purposes.

## Limitations

- Strict implementation of SRX syntax in Java is not possible — see the **SRX and Java** page for details.
- Using **Renumber Code IDs** when creating a translation kit requires a separate Desegmentation Step when merging back, and may not be possible with segment-based formats like TTX.
- Text units flagged as non-translatable or with no content are never segmented.

## Notes

- Segmentation uses SRX 2.0 rules — regular expressions define break and non-break positions.
- By default, already-segmented text units are not re-segmented. Use `overwriteSegmentation` or `deepenSegmentation` to change this.
- The three pre-segmented behaviors (keep, overwrite, deepen) are mutually exclusive.
- When `forceSegmentedOutput` is enabled, it modifies the downstream filter's output option to represent segments.

## Examples

### Default sentence segmentation
Segments source text into sentences using SRX rules with segment verification enabled:
```yaml
segmentSource: true
segmentTarget: false
checkSegments: true
forceSegmentedOutput: true
```

### Re-segment with new rules
Overwrites existing segmentation with new SRX rules:
```yaml
segmentSource: true
overwriteSegmentation: true
forceSegmentedOutput: true
```

### Deepen existing segmentation
Applies finer-grained rules to split segments further:
```yaml
segmentSource: true
deepenSegmentation: true
forceSegmentedOutput: true
```
