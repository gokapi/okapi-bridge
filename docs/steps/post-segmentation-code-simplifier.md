# Post-segmentation Inline Codes Simplifier

This step simplifies inline codes in text units by merging adjacent codes and optionally moving leading/trailing codes out of segments into inter-segment TextParts. **It must be run after segmentation.** Original (un-merged) codes are preserved as `okp:merged` attributes in the generated XLIFF file, and trimmed codes are written outside `mrk` elements.

## Parameters

#### Move leading and trailing codes to skeleton

Moves leading and trailing inline codes out of each segment and places them in inter-segment TextParts (outside `mrk` elements in XLIFF). This reduces noise for translators by keeping non-translatable codes outside the segment boundaries. The codes are not deleted — they are repositioned so the final output document remains identical.

> **Note:** Does not apply to bi-lingual formats (XLIFF, TMX, TTX) where source and target codes must align by ID.

#### Merge adjacent codes

Joins adjacent inline codes within each segment into a single code, simplifying the segment for translation by reducing the number of individual code placeholders. Original (un-merged) codes are preserved as `okp:merged` attributes in the generated XLIFF file, allowing the merge to be reversed if needed.

> **Note:** Does not apply to bi-lingual formats (XLIFF, TMX, TTX) where source and target codes may differ and must align by ID.

## Limitations

- Bi-lingual formats (XLIFF, TMX, TTX, etc.) will **not** have their codes simplified, because codes may differ between source and target and must align by ID.
- The step must be placed in the pipeline **after** segmentation — it operates on linguistically segmented text units.

## Notes

- Original un-merged codes are saved as `okp:merged` attributes in the generated XLIFF file, enabling reversibility.
- Trimmed (leading/trailing) codes are written outside `mrk` elements in the XLIFF output.
- The step examines each linguistically distinct segment within a TextUnit independently.
