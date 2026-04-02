# Segments to Text Units Converter

Converts segmented text units (paragraphs) into multiple text units, one per aligned sentence pair, for each target locale. This is useful for steps like the Diff Leverage Step which require un-segmented text units. Text units that reference other `IResource` objects or are referents are passed through unchanged.

## Parameters

This step has no parameters.

## Limitations

- Using this step may prevent merging, so it should only be used in pipelines that do not require translated translation kits to be merged.
- Text units that refer to another `IResource` or are referents are passed through as-is, even if they contain segments.

## Notes

- This step is commonly paired with the Diff Leverage Step, which requires un-segmented text units to function correctly.
- The conversion operates on all target locales present in the document.

## Examples

### Converting segmented paragraphs for diff leverage

A typical use case is placing this step before the Diff Leverage Step in a pipeline:

1. Segmentation Step — segments paragraphs into sentences
2. **Segments to Text Units Converter** — converts each segment pair into its own text unit
3. Diff Leverage Step — performs leverage on the now-individual text units
