# Character Count Step

Performs character counts on different parts of a set of documents, following the [GMX-V 2.0 standard](http://www.xtm-intl.com/manuals/gmx-v/GMX-V-2.0.html). Character counts are saved as annotations on text units and optionally on higher-level resources (batches, documents, groups). These annotations can be consumed by other steps such as the Scoping Report Step to generate reports.

Takes: Filter events. Sends: Filter events.

## Parameters

A character count annotation is always set on each text unit. The following options control additional aggregation levels:

#### Batches
Adds a character count annotation aggregated across the entire batch. Useful for getting a total count across all documents processed in a pipeline run. Enabled by default.

#### Batch Items
Adds a character count annotation for each batch item. A batch item typically corresponds to a single input/output document pair in the pipeline. Enabled by default.

#### Documents
Adds a character count annotation for each document. Enable this when you need per-document character counts, e.g. for per-file reporting. Disabled by default.

#### Sub-documents
Adds a character count annotation for each sub-document. Sub-documents occur when a filter extracts embedded content (e.g. HTML inside an XML file). Disabled by default.

#### Groups
Adds a character count annotation for each group resource. Groups are logical collections of text units defined by the filter (e.g. table rows, list items). Disabled by default.

#### Size of Text Buffer
Controls the size of the internal text buffer used during counting. A value of `0` uses the default buffer size. Increase for very large documents if memory permits.

## Notes

- Counts follow the GMX-V 2.0 standard.
- Count annotations are stored as resource-level annotations consumable by downstream steps such as the Scoping Report Step.
- A per-text-unit annotation is always generated regardless of parameter settings.

## Limitations

None known.

## Examples

Enable batch and document-level counts, then feed the results into the Scoping Report Step to generate a per-document word/character count report.
