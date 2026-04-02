# Diff Leverage Step

The Diff Leverage step compares the source text of two (or three) documents to leverage existing translations for new source content. It matches text units between an old bilingual document and a new document, copying old target translations when source segments match above a configurable threshold. Supports both bilingual input (two documents) and monolingual input (three documents with aligned text units).

Takes: Filter events. Sends: Filter events.

## Input Modes

**Two-document mode (bilingual):**
1. New document that needs to be translated
2. Bilingual document with old source and its corresponding old translation

**Three-document mode (monolingual):**
1. New document that needs to be translated
2. Document with the old source
3. Document with the old translation

When using three documents, text units between the second and third must be aligned (same number, same order).

## Parameters

#### Leverage only if the match is equal or above this score

Controls the minimum match quality required before an old translation is leveraged. A score of **100** means only exact matches (both text and inline codes) are leveraged. Lower values allow fuzzy matches — the old target is copied even when the new source differs slightly from the old source. Valid values: integer between **1** and **100**.

- `100` — Only leverage exact matches (default)
- `95` — Allow minor differences (e.g., punctuation changes)
- `80` — Allow moderate differences for rough leveraging

#### Include inline codes in the comparison

Controls whether inline codes (formatting tags, placeholders) are included when comparing the old and new source text. When enabled, codes must also match for the text unit to reach the leverage threshold. When disabled, only the textual content is compared, ignoring any differences in inline codes.

> **Note:** When the fuzzy threshold is set to 100 and this option is enabled, both text and codes must match exactly for leveraging to occur.

#### Diff only and mark the TextUnit as matched

Runs the comparison without copying translations or creating leverage annotations. Text units that match above the threshold are marked as matched, but no target content is modified. Useful for previewing what would be leveraged before committing to changes.

#### Copy to/over the target

Copies the matched old target content directly into the target of the new document. A leverage annotation is still created.

> **Warning:** Copied target will not be segmented and any existing target will be lost. This is a destructive operation on existing target content. Has no effect when "Diff only" is enabled.

## Limitations

- The comparison process is done on the full list of text units **in memory** — very large documents may not be processable.
- When using three monolingual inputs, text units between the old source and old translation must be aligned (same number, same order).

## Notes

- Text units (paragraphs) must align across all input documents for correct comparison.
- Designed for use within a filter pipeline — takes filter events as input and sends filter events as output.
