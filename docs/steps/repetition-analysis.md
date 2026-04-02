# Repetition Analysis

Analyzes repetitions across input documents, performing either exact or configurable fuzzy matching on source segments. For each group of repetitive segments, it attaches `RepetitiveSegmentAnnotation` to the source and `AltTranslationsAnnotation` to the corresponding target, enabling downstream steps (like leveraging or counting) to act on repeated content.

**Takes:** Filter events | **Sends:** Filter events

## Annotations

Two types of annotations are created for found repetitive segments:

- **RepetitiveSegmentAnnotation** — attached to a repetitive source segment.
- **AltTranslationsAnnotation** — attached to the target segment corresponding to a repetitive source segment. Not attached for the first repetitive segment in its group, to avoid being counted twice as repetitive with itself.

## Parameters

#### Fuzzy Threshold

Fuzzy threshold for fuzzy repetitions. Leave at 100 for exact repetitions only. Lower values enable fuzzy matching with decreasing similarity requirements.

## Limitations

No known limitations.

## Notes

- The first segment in each repetition group is intentionally excluded from `AltTranslationsAnnotation` to prevent double-counting by downstream counting steps.
- This step expects filter events and passes them through, making it suitable for insertion into a filter-event pipeline between extraction and merging stages.
