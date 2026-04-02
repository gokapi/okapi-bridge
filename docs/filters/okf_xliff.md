# XLIFF Filter

The XLIFF Filter processes **XLIFF 1.2** (XML Localisation Interchange File Format) documents, an OASIS standard for transporting translatable text between translation tools. It supports standard XLIFF, SDLXLIFF, and IWS XLIFF variants with dedicated writer modes for each. The filter handles `<seg-source>` segmentation, `<alt-trans>` translation matches, ITS annotations, and maps XLIFF metadata to Okapi resource properties.

Supported file extensions: `.xlf`, `.xlif`, `.xliff`, `.mxliff`, `.mqxliff`, `.sdlxliff`

## Parameters

### Extraction

#### Preserve Whitespace by Default
Controls the default whitespace handling when `xml:space` is not specified on a `<trans-unit>`. When enabled, whitespace is preserved by default (as if `xml:space=\"preserve\"` were set). When disabled, content is unwrapped.

#### Include XLIFF Extension Elements
Includes non-standard XLIFF extension elements from custom namespaces. Typically needed for vendor-specific XLIFF variants.

#### Include ITS Annotations
Includes ITS (Internationalization Tag Set) and ITSXLF annotations in the extracted data.

#### Treat CDATA as Inline
Treats CDATA sections found within translatable content as inline codes rather than text content.

#### Process Sub Elements as Text Units
Processes `<sub>` elements inside inline codes as separate text units rather than including them in the parent code's data. By default, `<sub>` content is included in the parent inline code and a warning is generated.

#### Ignore Existing Segmentation
Discards all segmentation information (`<seg-source>` elements) from the input. Segmented content becomes unsegmented. **Warning:** Any `<alt-trans>` data attached to individual segments is lost.

#### Skip Seg-Source Without Markers
Skips `<seg-source>` elements that have no `<mrk mtype='seg'>` segment markers.

#### Fall Back to Trans-Unit ID
Uses the `id` attribute of `<trans-unit>` as the text unit name when `resname` is not present. Useful for XLIFF documents that use `resname`-like values for `id` but omit the `resname` attribute.

#### Force Unique Text Unit IDs
Ensures all text unit IDs are unique across the document.

### Output

#### Add Target Language
Adds the `target-language` attribute to `<file>` elements if not present.

#### Override Target Language
Overrides the target language throughout the document — sets `target-language` and `xml:lang` in all `<target>` elements to the user-specified language. Useful for template-based workflows.

> **Note:** May result in `<alt-trans>` elements that no longer match the trans-unit's target language.

#### Allow Empty Targets
Prevents copying source text into empty `<target>` elements.

#### Always Add Targets
Always adds `<target>` elements to trans-units, even when none existed in the input.

#### Always Use Seg-Source
Always includes `<seg-source>` elements in output.

#### Output Segmentation Type
Controls segmentation representation in output:
- **0** — Preserve input segmentation
- **1** — Always segment (add `<seg-source>` even for unsegmented input)
- **2** — Never segment (remove all `<seg-source>`)
- **3** — Segment only multi-segment entries

### Translation States

#### Target State Mode
Controls how the `state` attribute on `<target>` elements is handled.

#### Target State Value
The value to set for the `state` attribute (e.g., `needs-translation`, `translated`, `final`).

#### Use Translation Target State
Uses translation-specific target state handling.

#### Add Alt-Trans Elements
Allows adding new `<alt-trans>` elements. Used with the Leveraging Step to include TM matches.

#### Use G Notation in Alt-Trans
Uses `<g>`/`<x>` notation instead of `<bpt>`/`<ept>`/`<ph>` in new alt-trans elements. Only applies when Add Alt-Trans is enabled.

#### Edit Existing Alt-Trans
Allows modification of existing `<alt-trans>` elements, treating them like added ones.

### Inline Codes

#### Enable Code Finder
Enables regex-based detection of additional inline codes in text content. Standard XLIFF inline elements are handled natively — this is for detecting additional patterns in the text content itself.

### Sub-Filters

#### CDATA Sub-Filter
An Okapi filter configuration ID to apply to CDATA content (e.g., `okf_html`).

#### PCDATA Sub-Filter
An Okapi filter configuration ID to apply to PCDATA content.

### Parser

#### Use Custom XML Parser
Uses Woodstox instead of the default JVM XML parser. **Recommended for SDLXLIFF** — the default parser can cause `StackOverflowError` on documents with large element content.

#### Factory Class
The fully-qualified class name for the custom parser factory (e.g., `com.ctc.wstx.stax.WstxInputFactory`).

### SDL XLIFF Extensions

#### Use SDL XLIFF Writer
Enables SDL-specific writer for proper SDLXLIFF round-tripping.

> **Note:** SDL properties (`locked`, `conf`, `origin`) are stored at the container level. With multiple segments, values come from the last segment. From M35, segment-level properties exist but are read-only.

#### Use Segments for SDL Properties
Reads SDL properties from individual segments rather than the text container level.

#### SDL Segment Confirmation Value
The confirmation status for output segments (e.g., `Translated`, `Draft`, `ApprovedTranslation`).

#### SDL Segment Locked Value
The locked status value for output segments.

#### SDL Segment Origin Value
The origin value for output segments (e.g., the TM or MT source).

### Idiom WorldServer Extensions

#### Use IWS XLIFF Writer
Enables IWS-specific writer for WorldServer XLIFF files.

#### Block Finished
Blocks trans-units with finished status, treating them as non-translatable.

#### Block Lock Status
Blocks trans-units with locked status.

#### Block Multiple Exact
Blocks trans-units with multiple exact TM matches.

#### Block TM Score
Blocks trans-units with TM scores at or above the threshold.

#### TM Score Threshold
The TM score threshold value (e.g., `100.00`).

#### Include Multiple Exact
Includes trans-units with multiple exact TM matches.

#### Remove TM Origin
Removes TM origin metadata from output.

#### Translation Status Value
The IWS status value to set (e.g., `finished`, `pending`).

#### Translation Type Value
The IWS type value to set (e.g., `manual_translation`).

### XTM Attributes
Process XTM-specific attributes in the XLIFF document.

## Limitations

- `<sub>` element content is not supported as translatable text by default — it is included in the parent inline code.
- `<mrk mtype='protected'>` is converted to an inline code. Nested `<mrk mtype='x-its-translate-yes'>` within it is not supported.
- For SDLXLIFF: SDL segment-level properties (`locked`, `conf`, `origin`) are read-only (from M35 onward).
- The default Java XML parser can cause `StackOverflowError` on large XLIFF documents. Use Woodstox.

## Notes

- Encoding defaults to UTF-8 if no declaration is present.
- BOM is preserved only when input was UTF-8 with BOM.
- Line breaks match the input document style.
- `<seg-source>` content must match `<source>` content (excluding markers) or segmentation is discarded.
- `<alt-trans>` entries are sorted by match type and score. `match-quality` is used as score if numeric.
- `maxbytes` maps to `its-storageSize` (note: `its:storageSize` is not recognized — use `maxbytes`).

## Examples

### SDLXLIFF Configuration
```json
{
  \"parser\": {
    \"useCustomParser\": true,
    \"factoryClass\": \"com.ctc.wstx.stax.WstxInputFactory\"
  },
  \"sdl\": {
    \"useSdlXliffWriter\": true,
    \"sdlSegConfValue\": \"Translated\"
  },
  \"extraction\": {
    \"preserveSpaceByDefault\": true,
    \"skipNoMrkSegSource\": true
  }
}
```

### Template for Multiple Target Languages
```json
{
  \"output\": {
    \"overrideTargetLanguage\": true,
    \"addTargetLanguage\": true
  }
}
```

### IWS XLIFF Configuration
```json
{
  \"iws\": {
    \"useIwsXliffWriter\": true,
    \"iwsBlockFinished\": true,
    \"iwsTransStatusValue\": \"pending\",
    \"iwsTransTypeValue\": \"manual_translation\"
  }
}
```
