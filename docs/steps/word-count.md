# Word Count Step

Performs word counts on different parts of a set of documents, saving results as annotations on filter events. Word count annotations are always set on each text unit, with optional aggregation at batch, batch item, document, sub-document, and group levels. The counts follow the [GMX-V 2.0 standard](http://www.xtm-intl.com/manuals/gmx-v/GMX-V-2.0.html) and can be consumed by downstream steps such as the Scoping Report Step.

Takes: Filter events. Sends: Filter events.

## Parameters

#### Batches

Adds a word count annotation aggregated across the entire batch. A batch represents the top-level grouping of all input documents in a pipeline run. The annotation can be consumed by downstream steps such as the **Scoping Report Step** to generate summary reports.

Default: **enabled**

#### Batch Items

Adds a word count annotation per batch item. Each batch item typically corresponds to one input file added to the pipeline. The annotation can be consumed by downstream steps such as the **Scoping Report Step** to generate per-item reports.

Default: **enabled**

#### Documents

Adds a word count annotation per document. A document corresponds to a `START_DOCUMENT` / `END_DOCUMENT` event pair from the filter. Useful when a single batch item produces multiple document events (e.g., multi-file archives).

Default: **disabled**

#### Sub-documents

Adds a word count annotation per sub-document. Sub-documents occur when a filter emits nested `START_SUBDOCUMENT` / `END_SUBDOCUMENT` events — for example, embedded content within an HTML or XLIFF file.

Default: **disabled**

#### Groups

Adds a word count annotation per group. Groups are sets of related text units emitted between `START_GROUP` / `END_GROUP` events — for example, table rows, list items, or other structural groupings defined by the filter.

Default: **disabled**

#### Text Buffer Size

Controls the size of the internal text buffer used during word counting. A value of `0` means the default buffer size is used. Only increase this if processing very large text units that exceed the default capacity.

Default: **0** (use default)

## Limitations

None known.

## Notes

- Word count annotations are **always** set on each text unit regardless of the checkbox options — the options control additional aggregation levels.
- Word counts follow the **GMX-V 2.0** standard (formerly GMX-V 1.0 / LISA).
- Annotations are stored on the filter events and passed downstream — this step does not produce any output files by itself.
- Commonly paired with the **Scoping Report Step** which reads the annotations and generates a word count report.

## Examples

### Basic word count with scoping report

Count words per batch item and document, then generate a report using the Scoping Report Step downstream in the pipeline.

```yaml
countInBatch: true
countInBatchItems: true
countInDocuments: true
countInSubDocuments: false
countInGroups: false
bufferSize: 0
```
