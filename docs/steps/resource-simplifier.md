# Resource Simplifier

Simplifies the resources of filter events by splitting generic skeletons into parts that contain no references. Original references are converted to skeleton parts or `TEXT_UNIT` events, and the resulting sequence is packed into a single `MULTI_EVENT` event. This step is format-specific and operates on filter events.

## Parameters

This step has no parameters.

## Processing Details

- The skeleton parts are attached to newly created `DOCUMENT_PART` events.
- Original references are converted either to skeleton parts or `TEXT_UNIT` events.
- The sequence of `DOCUMENT_PART` and `TEXT_UNIT` events is packed into a single `MULTI_EVENT` event.
- The simplification algorithm is format-specific — behavior varies depending on the filter that produced the events.

## Limitations

None known.
