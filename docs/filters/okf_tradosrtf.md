# Trados-Tagged RTF Filter

The Trados-Tagged RTF Filter reads RTF files prepared with the Trados translation layer of styles. It extracts segmented text from Trados-Tagged RTF documents where HTML/XML tags are visible and marked with `tw4wInternal` (red) or `tw4wExternal` (gray) styles. **This is a read-only filter** — it cannot merge translations back into the original document.

A Trados-Tagged RTF document is an RTF file that has special styles associated to different parts of the content and has segmentation markers. Note that a "normal" RTF file translated with Trados (showing segments with native RTF formatting like bold) is **not** a full Trados-Tagged RTF. A true Trados-Tagged RTF shows the actual tags (e.g., `<b>` and `</b>`) marked with the `tw4wInternal` style.

## Parameters

This filter has no configurable parameters.

## Encoding

The filter determines the input encoding using the following logic:

1. The document declares encoding information for each font, including a default encoding.
2. If no default encoding is declared in the document, the user-specified encoding is used as the fallback.

## Limitations

- **Read-only filter** — does not generate output corresponding to the input. Must be used for input/extraction only.
- Only understands inline codes represented with Trados styles (`tw4wInternal`, `tw4wExternal`), not RTF with "normal" formatting.
- Any text outside Trados segmentation markers is ignored and treated as non-translatable.

## Notes

- Use this filter only in extraction/reading pipelines — do not expect merged output.
- Ensure your RTF files are true Trados-Tagged RTF (with visible tagged styles), not standard Trados-translated RTF with native formatting.
