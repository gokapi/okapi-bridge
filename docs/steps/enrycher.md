# Enrycher Step

Invokes the [Enrycher Web service](http://enrycher.ijs.si/) to provide ITS disambiguation annotations in the source text content. The step sends groups of source entries to the Enrycher service, receives ITS-annotated results, and maps them back to Okapi inline annotations usable by downstream steps. Implements the [ITS 2.0 Text Analysis data category](http://www.w3.org/TR/its20/#Disambiguation).

> **Note:** The amount of annotations added to the content can be very high depending on the type of text.

## Parameters

#### URL of the Enrycher Web service

The endpoint URL of the Enrycher Web service instance to use for disambiguation annotations. The default points to the public demo service hosted at IJS (`http://aidemo.ijs.si/mlw/`).

> **Note:** The public Enrycher service (as of Aug-18-2013) supports only **English** and **Slovenian**.

#### Events buffer

Controls how many filter events are buffered before sending a batch query to the Enrycher Web service. Higher values send more segments at once, which generally **improves speed** but increases **memory usage**. Tune this based on document size and available memory.

Valid values: Positive integer. Default `20`. Larger values improve throughput at the cost of higher memory consumption.

> **Note:** The more events are buffered, the more memory is required. For large documents, consider reducing this value if you encounter memory issues.

## Limitations

- This step is **BETA**.
- The Enrycher Web service (as of Aug-18-2013) supports only **English** and **Slovenian**.
- The amount of annotations added to the content can be very high depending on the type of text.

## Notes

- Events are buffered and sent in batches to the Web service rather than one at a time, controlled by the `maxEvents` parameter.
- Annotations are mapped back to Okapi inline annotations that can be consumed by any downstream pipeline steps.
- Requires both `source-language` and `target-language` to be set in the pipeline.
