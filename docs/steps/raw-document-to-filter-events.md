# Raw Document to Filter Events Step

This step extracts the content of a raw document into a set of filter events using an associated filter configuration. It is typically the first step in a pipeline, converting raw document input into structured filter events that downstream steps can process. To reverse the conversion, use the **Filter Events to Raw Document Step**.

## Parameters

This step has no user-configurable parameters. All parameters are provided by the calling application (filter configuration, input encoding, etc.).

## Limitations

No known limitations.

## Notes

- This step is typically the first step in a pipeline, but it can be placed after any step that sends a raw document.
- Requires both source and target language to be set in the pipeline context.

## Examples

#### Minimal extract-then-merge pipeline

The simplest useful pipeline extracts a document into filter events and merges them back:

```yaml
steps:
  - raw-document-to-filter-events
  - filter-events-to-raw-document
```

This baseline pipeline can be extended by inserting processing steps between extraction and merging.
