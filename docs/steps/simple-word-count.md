# Simple Word Count

Performs word counts on the source content of text units across a set of documents. Designed for fast, simple counting — for more sophisticated counts, see the **Word Count Step**. Word counts are saved as annotations that can be consumed by downstream steps such as the **Translation Comparison Step**.

## Parameters

This step has no parameters.

## Limitations

- No known limitations.

## Notes

- Word counts are stored as **annotations** on each text unit, making them available to downstream steps (e.g., Translation Comparison Step can include word counts in its report).
- Only counts **source** content — target content is not counted.
- This is a lightweight counting step; for locale-aware or customizable counting logic, use the **Word Count Step** instead.

## Examples

### Using word count annotations in a pipeline

Place the Simple Word Count step before the Translation Comparison Step so that word count annotations are available for the comparison report.

```
Raw Document → Filter → Simple Word Count → Translation Comparison
```

The Translation Comparison Step will automatically pick up the word count annotations produced by Simple Word Count.
