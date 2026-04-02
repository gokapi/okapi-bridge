# ICML / WCML Filter

This filter processes ICML (InCopy Markup Language) and WCML documents, which are XML-based formats used by Adobe InDesign and InCopy. It handles both `.icml` and `.wcml` file extensions. The filter is very similar to the [IDML Filter](IDML_Filter) and supports extraction of notes, master spreads, and configurable inline code simplification.

## Parameters

### Extraction

#### Extract Notes
Extracts the content of `<Note>` elements in the ICML/WCML document as translator notes. Note content is typically editorial annotations added by authors or editors in InCopy/InDesign and may contain context useful for translators.

#### Extract Master Spreads
Extracts the content of master spreads if they exist in the document. When disabled, only normal spreads are extracted. Master spreads contain repeating page elements (headers, footers, page numbers) that appear across multiple pages in the InDesign layout.

#### Simplify Inline Codes When Possible
Reduces the number of inline codes by re-grouping adjacent codes when possible. InDesign documents often contain many formatting runs that produce excessive inline codes. Simplifying merges adjacent codes into fewer placeholders, making segments easier for translators to work with.

#### Maximum Spread Size
Maximum size for spread files in KBytes (range 1–32000, default 1000). Any spread file above this value will either generate an error or be skipped from extraction. This allows you to skip large spread files that may contain only graphics and require too much memory to open.

> **Note:** Skipped files are **not** checked for translatable text — content in oversized spreads will be silently omitted from extraction.

#### Create New Paragraphs on Hard Returns
Controls whether `<Br/>` (hard return / break) elements create a new text unit (segment boundary). By default, content separated by `<Br/>` tags remains in the same text unit. Enabling this splits the text at each break into separate segments for translation.

### Inline Codes

#### Enable Inline Code Detection
Enables pattern-based detection of inline codes using regex rules. When enabled, text matching the defined patterns will be treated as non-translatable inline codes.

#### Code Finder Rules
Defines regex patterns to identify inline codes within translatable text. Useful for protecting variables, formatting codes, or placeholder tokens.

#### Merge Adjacent Codes
Merges consecutive inline codes into a single placeholder, reducing clutter in segments.

#### Move Boundary Codes
Moves inline codes at the start or end of a segment into the non-translatable skeleton.

#### Simplifier Rules
Rules for simplifying inline code representation into simpler placeholder forms.

## Limitations

- Processing details for WCML/ICML are not fully documented in the Okapi wiki.

## Notes

- The filter handles both `.icml` and `.wcml` file extensions with the same processing logic.
- The filter is very similar to the IDML Filter but operates on single-file ICML/WCML documents rather than IDML packages.
- ICML (InCopy Markup Language) is an XML-based format used by Adobe InDesign and InCopy.

## Examples

### Skip Large Graphic-Heavy Spreads

Configure the filter to skip spread files larger than 500 KB:

```yaml
extraction:
  skipThreshold: 500
```

### Extract Notes with Simplified Codes

```yaml
extraction:
  extractNotes: true
  simplifyCodes: true
  extractMasterSpreads: true
```"
