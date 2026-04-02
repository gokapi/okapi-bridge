# Pensieve TM Filter

The Pensieve TM Filter implements the `IFilter` interface for Pensieve TMs. It allows using a Pensieve TM as an input or output document, similar to other translatable document formats such as TMX, PO, etc.

## Parameters

This filter has no parameters.

## Limitations

- The `setOutput(OutputStream output)` method of `PensiveFilterWriter` is not implemented because Pensieve TM content is binary. You cannot output to other stream types without going through a format conversion.

## Notes

- The filter always uses its internal encoding — any user-specific encoding setting is ignored."
