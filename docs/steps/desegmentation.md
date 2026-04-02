# Desegmentation Step

This step joins all segments of each source and/or target text unit into a single content, effectively reversing the segmentation process. Non-segmented entries and text units flagged as non-translatable are left untouched. It operates on filter events and is typically used after processing to recombine segments that were split by the Segmentation step.

## Parameters

#### Join all segments of the source text

Joins all segments within each source text unit back into a single content.

Non-segmented entries and non-translatable text units are left untouched regardless of this setting.

#### Join all segments of the target text

Joins all segments within each target text unit back into a single content. Only applies when a target text exists.

Non-segmented entries and non-translatable text units are left untouched regardless of this setting.

#### Restore original IDs to renumbered codes

Restores the original inline code IDs after desegmentation. When content was segmented using the **Renumber code IDs** option in the Segmentation step, code IDs were renumbered per-segment starting at `1`. This option reverses that renumbering by reconstructing the original single sequence of IDs across the joined content.

> **Note:** This option is only meaningful when content was previously segmented with the **Renumber code IDs** option in the Segmentation step.

## Limitations

None known.

## Notes

- Non-segmented entries are passed through untouched.
- Text units flagged as non-translatable are passed through untouched.
- This step is typically paired with the Segmentation step in a pipeline — segmentation before processing and desegmentation after.
