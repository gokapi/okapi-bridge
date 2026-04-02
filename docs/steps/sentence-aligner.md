# Sentence Alignment Step

This step aligns sentences within text units (paragraphs) from two synchronized documents. It uses a character-length-based algorithm that accounts for language-specific expansion/contraction ratios, producing alignment match types such as 1-1, 2-1, 1-2, 0-1, 1-0, 2-3, 3-2, etc. The step can optionally segment content using default or custom SRX rules before alignment, and outputs either bilingual text units or a TMX file.

## Parameters

### Output

#### Generate the following TMX document

Controls the output mode of the step:

- **Enabled (default)**: Produces a TMX file at the specified path. The step passes through the original events (possibly re-segmented).
- **Disabled**: The step returns bilingual text units corresponding to the alignment (also segmented), replacing the original events in the pipeline.

> **Note:** When generating TMX, the original filter events are passed through unchanged (except for possible re-segmentation). The aligned data goes only to the TMX file.

#### TMX output path

Path where the aligned TMX document will be written. If the file already exists it will be **overwritten**. Default: `aligned.tmx`.

> **Warning:** Existing files at this path will be overwritten without confirmation.

### Source Segmentation

#### Segment the source content

Segments source content before alignment using SRX rules. If disabled, the content is expected to be already segmented. If enabled and the content is already segmented, the existing segmentation will be reset and replaced.

#### Use custom source segmentation rules

Uses a specified SRX file instead of the built-in default rules for source segmentation.

> **Note:** The default rules are hard-coded within the step — they are NOT the rules from the `config` sub-directory of the installation directory.

#### SRX path for the source

Full path to an SRX document defining segmentation rules for the source language.

### Target Segmentation

#### Segment the target content

Segments target content before alignment using SRX rules. If disabled, the content is expected to be already segmented. If enabled and the content is already segmented, the existing segmentation will be reset and replaced.

#### Use custom target segmentation rules

Uses a specified SRX file instead of the built-in default rules for target segmentation.

> **Note:** The default rules are hard-coded within the step — they are NOT the rules from the `config` sub-directory of the installation directory.

#### SRX path for the target

Full path to an SRX document defining segmentation rules for the target language.

### Alignment Options

#### Collapse whitespace

Normalizes all whitespace characters (spaces, newlines, tabs, etc.) to a single space before segmentation and alignment. This can improve alignment accuracy when source and target documents have inconsistent whitespace formatting.

#### Output 1-1 matches only

Filters the output to include only 1-to-1 sentence alignments, discarding all other match types (2-1, 1-2, 0-1, 1-0, etc.). Useful when you need high-confidence aligned pairs for TM building.

#### Force Simple One to One Alignment

For each paragraph: if source and target have the same number of sentences, aligns them 1-to-1. If the counts differ, joins all sentences in the paragraph back together and outputs the aligned paragraph as a single unit.

> **Note:** When enabled, this overrides the *Output 1-1 matches only* option.

## Limitations

- Currently the events sent by this step are the same as the events it received — aligned text units are not yet passed through the pipeline (only written to TMX).
- Source and target text units **must be perfectly synchronized** (paragraph-aligned). Mismatched text unit counts will cause an error.
- Entries set as non-translatable are not processed.

## Notes

- The alignment algorithm uses character-length ratios with internal parameters that account for language-specific expansion/contraction.
- When processing bilingual documents (e.g., TMX, PO), use only a single input list.
- The default SRX segmentation rules are hard-coded within the step — they are not the rules from the `config` sub-directory of the installation directory.
