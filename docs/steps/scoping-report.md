# Scoping Report Step

This step creates a template-based report on various counts (word count, character count, etc.) and optionally leveraged data. It acts as a presentation layer, displaying information provided by other steps such as the Word Count Step, Character Count Step, or Leveraging Step. Reports can be generated in plain text or HTML format using customizable templates with report field placeholders.

To include leveraging statistics, the pipeline must include a leveraging step (e.g. Leveraging Step) prior to this step. For just generating word- or character-count annotations without a report, use the Word Count Step or Character Count Step instead.

## Parameters

### General

#### Name of the project
The project name appears in the report title and is available as the `[PROJECT_NAME]` template field. This is a display-only label.

#### Custom template URI
Path or URI to a custom report template file. Templates contain text mixed with report field placeholders enclosed in brackets (e.g. `[PROJECT_NAME]`, `[ITEM_TOTAL_WORD_COUNT]`). Table rows use double brackets: `[[col1],[col2],]`.

If left empty or the URI is not found, the default HTML template is used.

> **Note:** If the specified URI is not found, the step silently falls back to the default template rather than raising an error.

#### Custom template string
Specify the report template directly as a string instead of referencing a file. Uses the same template syntax as file-based templates. If `customTemplateURI` is also specified, the URI takes precedence.

#### Output path
Full path for the generated report file. Supports variable substitution:
- `${rootDir}` — the pipeline root directory
- `${srcLoc}` — source locale
- `${trgLoc}` — target locale

### GMX Non-Translatable Categories

These flags control which GMX word/character count categories are treated as non-translatable in the report's translatable/non-translatable split.

#### GMX Protected Count
Words/characters in text marked as 'protected' or not translatable (e.g. XLIFF `<mrk mtype="protected">` elements). **Default: enabled.**

#### GMX Exact Matched Count
Words/characters matched unambiguously with a prior translation. Provided by the Leveraging Step. **Default: enabled.**

#### GMX Leveraged Matched Count
Words/characters matched against a leveraged TM database. Provided by the Leveraging Step. **Default: disabled.**

#### GMX Repetition Matched Count
Words/characters in repeating text units. Repetition matching takes precedence over fuzzy matching. Provided by the Repetition Analysis Step. **Default: disabled.**

#### GMX Fuzzy Matched Count
Words/characters fuzzy-matched against a leveraged TM. Provided by the Leveraging Step. **Default: disabled.**

#### GMX Alphanumeric Only Text Unit Count
Words/characters in text units containing only alphanumeric words. **Default: enabled.**

#### GMX Numeric Only Text Unit Count
Words/characters in text units containing only numeric words. **Default: enabled.**

#### GMX Measurement Only Text Unit Count
Words/characters from measurement-only text units. **Default: enabled.**

### Okapi Non-Translatable Categories

These flags control which Okapi match categories are treated as non-translatable.

#### Exact Unique Id Match
Matches EXACT and a unique ID. **Default: enabled.**

#### Exact Previous Version Match
Matches EXACT from the immediately preceding document version (v3 for v4, not v2). **Default: enabled.**

#### Exact Local Context Match
Matches EXACT plus surrounding segments. **Default: disabled.**

#### Exact Document Context Match
Matches EXACT from the same document. **Default: disabled.**

#### Exact Structural Match
Matches EXACT plus structural segment type (title, paragraph, etc.). **Default: disabled.**

#### Exact Match
100% match on text and codes. **Default: disabled.**

#### Exact Text Only Previous Version Match
Text-only exact match from a previous document version. **Default: disabled.**

#### Exact Text Only Unique Id Match
Text-only exact match with unique ID. **Default: disabled.**

#### Exact Text Only
Text matches exactly but inline codes differ. **Default: disabled.**

#### Exact Repaired
Exact match after automated repair (number replacement, code repair, etc.). **Default: disabled.**

#### Fuzzy Previous Version Match
Fuzzy match from a previous document version. **Default: disabled.**

#### Fuzzy Unique Id Match
Fuzzy match with unique ID. **Default: disabled.**

#### Fuzzy Match
Partial match on text and/or codes. **Default: disabled.**

#### Fuzzy Repaired
Fuzzy match with automated repair applied. **Default: disabled.**

#### Phrase Assembled
Translations assembled from TM phrases. **Default: disabled.**

#### MT
Machine translation output. **Default: disabled.**

#### Concordance
TM concordance or phrase match (usually word/term level). **Default: disabled.**

## Templates

Templates contain text and report field placeholders. Report fields are enclosed in brackets (e.g. `[PROJECT_NAME]`). Table rows use double brackets to iterate over project items: `[[ITEM_NAME],[ITEM_TOTAL_WORD_COUNT],]`.

Available field prefixes:
- `PROJECT_` — aggregated across all items
- `ITEM_` — per-file values

Character count variants are available by replacing `WORD` with `CHARACTER` or appending `_CHARACTER`.

## Notes

- The step is a **presentation layer** — count data must be provided by preceding pipeline steps. Missing steps result in zeros.
- The `countAsNonTranslatable_*` flags control which categories are subtracted from total counts to compute translatable vs non-translatable word/character counts.
- The step passes filter events through unchanged — it only reads annotations to generate the report.

## Examples

### CSV-style plain text template

A minimal template producing a CSV-like report with per-file leveraging:

```
Project Name: [PROJECT_NAME]
Creation Date: [PROJECT_DATE]
Target Locale: [PROJECT_TARGET_LOCALE]

File,Exact Previous Version,100% Matches,Fuzzy,Total,
[[ITEM_NAME],[ITEM_EXACT_PREVIOUS_VERSION],[ITEM_EXACT],[ITEM_FUZZY],[ITEM_TOTAL_WORD_COUNT],]
Total,[PROJECT_EXACT_PREVIOUS_VERSION],[PROJECT_EXACT],[PROJECT_FUZZY],[PROJECT_TOTAL_WORD_COUNT]
```
