# Paragraph Alignment

Aligns paragraphs (TextUnits) from two documents — a source and a target. Internally uses `ResourceSimplifier` to flatten events and expand TextUnits so all are available for alignment. Only TextUnit events are passed along; all other event types are discarded. Sends a special `PIPELINE_PARAMETERS` event to inform subsequent steps (e.g., Sentence Aligner) that the target input has been consumed.

## Parameters

#### Output 1-1 Matches Only?

Controls whether only cleanly paired (1-to-1) paragraph alignments are output, or whether many-to-many alignments are also included. When enabled, paragraphs that could not be matched to exactly one counterpart are dropped from the output. This produces a cleaner but potentially smaller set of aligned pairs.

Default: `true`

#### Use Skeleton Alignment? (Experimental)

Uses the document skeleton (structural markup surrounding text) as additional anchor points during alignment. Standard Gale & Church paragraph alignment can drift on long runs of paragraphs. Skeleton alignment exploits formatting cues to provide better anchoring, which may improve results for markup-rich formats (HTML, XML, DITA, etc.).

Default: `false`

> **Note:** Experimental — results may vary depending on the document format and how much structural markup is present.

## Limitations

- Standard Gale & Church alignment is not accurate for long runs of paragraphs without additional anchoring.
- Only TextUnit events are passed through — all other event types (document parts, skeleton, sub-documents) are lost.
- The target input is consumed by this step and is no longer available to subsequent steps.

## Notes

- `ResourceSimplifier` is called internally to flatten events and expand TextUnits before alignment.
- A special `PIPELINE_PARAMETERS` event is sent downstream to inform subsequent steps (e.g., Sentence Aligner) that the target input has been consumed.
