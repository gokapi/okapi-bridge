# TTX Filter

The TTX Filter processes **Trados TTX documents**, an XML-based bilingual format used by some versions of Trados tools and supported by several other translation tools. There are no official public specifications for TTX. The filter handles extraction of source and target text, respects existing segmentation, and carries over match percentages and translation annotations.

## Parameters

### Extraction Mode

Controls how existing TTX segments affect extraction:

- **Auto-detect existing segments**: Scans the file for existing `<Tu>` segments. If at least one is found, only segmented text is extracted. If none are found, all text is extracted.
- **Extract only existing segments**: Only existing segments are extracted. If the file has no pre-segmented content, nothing is extracted.
- **Extract all**: Extracts all text, whether it is in existing segments or not. Unsegmented text is wrapped into new segments.

> **Note:** Segmentation is often a cause for interoperability issues. For better compatibility with the tool that created the TTX files, it is recommended to work with pre-segmented documents.

In extract-all mode, contiguous text across segment boundaries is merged into a single text unit with multiple segments. For example:

```xml
<Raw>
Part 1 <Tu MatchPercent="0">
<Tuv Lang="EN">Part 2</Tuv></Tu> Part 3.
</Raw>
```

Extract all produces: `[Part 1 ][Part 2][ Part 3]` (one text unit, three segments)
Existing segments only produces: `[Part 2]`

### Escape Greater-Than Characters

Escapes all `>` characters as `&gt;` in the output document. By default, XML does not require `>` to be escaped, but some downstream tools may expect it.

## Processing Details

- **Input encoding**: Determined from the document's encoding declaration; falls back to UTF-8 if none is present.
- **Output BOM**: If input was UTF-8 with a BOM, the output preserves the BOM. If input was not UTF-8, no BOM is written.
- **Line-breaks**: Line-break style from the original input is preserved in the output.
- **Existing translations**: Segmented entries with translations extract both source and target. Match types are mapped based on `MatchPercent`: >100 with `Origin=xtranslate` → `EXACT_LOCAL_CONTEXT` (XU matches), >99 → `EXACT`, 1–99 → `FUZZY`. The `Origin` attribute is carried over to annotations.

## Limitations

- The TTX `<df>` element may cause problems when it spans across external tag boundaries (e.g., bold formatting spanning across paragraph boundaries in extracted HTML).
- No official public specifications exist for the TTX format.

## Examples

### Extract-all vs existing-segments-only

Given this TTX content:

```xml
<Raw>
Part 1 <Tu MatchPercent="0">
<Tuv Lang="EN">Part 2</Tuv></Tu> Part 3.
</Raw>
```

- **Extract all**: `[Part 1 ][Part 2][ Part 3]` — single text unit with three segments
- **Existing only**: `[Part 2]` — only the pre-segmented part
