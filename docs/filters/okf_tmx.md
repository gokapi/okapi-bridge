# TMX Filter

The TMX Filter implements the `IFilter` interface for **TMX (Translation Memory eXchange)** documents. TMX is a LISA standard for transporting translation memory data between translation tools. The filter reads `<tu>` entries with configurable target and segmentation behavior, and supports the [TMX 1.4b specification](https://www.gala-global.org/tmx-14b).

## Parameters

#### Read All Target Entries
Reads all target `<tuv>` elements into the text unit rather than only the selected target language. When disabled, only the selected target `<tuv>` is read and all remaining target entries become part of the skeleton.

> **Note:** The effect of this setting depends on whether downstream pipeline steps can process multiple targets.

#### Consolidate Document Part Skeleton
Consolidates skeleton parts from document-part resources and sends fewer events through the pipeline. This is sufficient for most use cases. Disable only if you need fine-grained access to individual document-part resources in custom pipeline steps.

#### Exit on Invalid TUs
Controls whether the filter stops processing when it encounters an invalid `<tu>` element. By default, invalid `<tu>` entries are skipped with a warning message.

> **Warning:** Skipping invalid entries risks producing an output file that doesn't match the input structure. Enable this to catch problems early.

#### Segment Creation Mode
Controls whether a segment is created for each extracted `<tu>` entry, based on the `segtype` attribute:

- **Always creates the segment** — Creates a segment regardless of `segtype`
- **Never creates the segment** — Never creates a segment, even if `segtype="sentence"`
- **Creates if segtype is 'sentence' or undefined** — Creates a segment when `segtype="sentence"` or when `segtype` is not set
- **Only if segtype is 'sentence'** — Creates a segment only when `segtype="sentence"`

#### Escape Greater-Than Characters
Escapes all `>` characters as `&gt;` in the output for stricter XML escaping.

#### Duplicate Property Separator
The string used to separate values when a TMX property appears more than once on the same element. Default: `", "`

## Limitations

- The `<sub>` element is not supported. When encountered, a warning is issued and the element content is merged into its parent element's content.
- The filter cannot reconstruct any DTD declaration from the original document.

## Notes

- **Input encoding:** Uses the document's encoding declaration if present; otherwise defaults to UTF-8 regardless of any specified default encoding.
- **Output BOM:** When output is UTF-8 and input was also UTF-8, a BOM is written only if one was detected in the input. If input was not UTF-8, no BOM is written.
- **Line breaks:** Output preserves the same line-break style as the original input.
