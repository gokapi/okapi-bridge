# Filter Events to Raw Document Step

This step merges filter events back into their original document format, effectively reversing the extraction performed by the Raw Document to Filter Events step. It takes filter events as input and produces a raw document as output. It is commonly paired with the Raw Document to Filter Events step to form a minimal extract-then-merge pipeline.

## Parameters

This step has no user-configurable parameters. All parameters are provided by the calling application.

## Limitations

None known.

## Notes

- If writing to a stream, the step sends `NO_OP` instead of a raw document.
- Requires both source and target language to be set in the pipeline.
- To create the initial filter events from an input document, use the **Raw Document to Filter Events Step**.

## Examples

### Minimal extract-merge pipeline

The simplest pipeline that extracts filter events from a document and immediately merges them back. This is useful as a roundtrip test to verify that a filter correctly reconstructs the original document format:

```
Raw Document to Filter Events Step
  +
Filter Events to Raw Document Step
```
