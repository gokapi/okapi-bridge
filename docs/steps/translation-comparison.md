# Translation Comparison Step

Compares translations between two or three documents, calculating similarity scores (ED-Score based on Levenshtein distance, and FM-Score based on Sørensen-Dice coefficient with 3-grams) for each text unit. The input documents **must have the same number of text units** in the **same order**. Outputs HTML reports with per-segment scores and summaries, and optionally a TMX document and tab-delimited score files.

## Scoring

Two similarity metrics are calculated per segment, both ranging from 0 (completely different) to 100 (identical):

- **ED-Score** — Edit Distance score based on the Levenshtein distance
- **FM-Score** — Fuzzy Match score based on Sørensen-Dice coefficient with 3-grams

### Summary Statistics

The HTML report summary includes:

- Total number of segments and words
- Average word count per segment
- Average ED-Score and FM-Score (by segment and by word)
- **Edit Effort Score** — measures post-editing effort on a 0–100 scale: `100 - (Avg ED-Score by word + Avg FM-Score by word) / 2`. A score of 100 means the translation was entirely re-written; 0 means no changes were made.

## Parameters

### Output Options

#### Generate output tables in HTML
Creates one HTML file per input document listing source text, translation text, and comparison scores. Also generates a tab-delimited `.txt` file with per-entry scores. The output file name is the first document name plus `.html`.

#### Opens the first HTML output after completion
Automatically opens the first generated HTML result file in the default browser when processing completes.

#### Use generic representation for inline codes
Controls how inline codes are displayed in the HTML report. When enabled, uses generic numbered tags like `<1>...</1>`. When disabled, shows original codes which can be long and complicated in some formats.

#### Generate a TMX output document
Creates a single TMX document containing all compared translations and their scores. The TMX path supports variables `${rootDir}`, `${inputRootDir}`, and locale variables (`${srcLoc}`, `${trgLoc}`, etc.).

#### TMX output path
Full path for the generated TMX file. Supports `${rootDir}`, `${inputRootDir}`, and locale variables.

### Language Suffixes

#### Suffix for target language code of document 2
Suffix appended to the target language code for document 2 entries in TMX output (e.g., `-mt` for machine translation). **Important:** If no suffix is provided, both target entries will have the same language code and only the last one will be kept.

#### Suffix for target language code of document 3
Suffix appended to the target language code for document 3 entries in TMX output.

### Document Labels

#### Label for document 1 / 2 / 3
Labels displayed in HTML reports and TMX output to identify each translation. Defaults are "Trans1", "Trans2", "Trans3".

### Comparison Options

#### Take into account case differences
When enabled, uppercase/lowercase differences affect similarity scores.

#### Take into account whitespace differences
When enabled, whitespace differences (extra spaces, tabs, line breaks) affect similarity scores.

#### Take into account punctuation differences
When enabled, punctuation differences affect similarity scores.

### XLIFF Alt-Trans

#### Use alt-trans for document 1
Uses the target from a specific `<alt-trans>` element in an XLIFF document as the baseline instead of the main `<target>`. This enables comparing a human translation with an MT candidate within the same XLIFF file.

#### Value in origin attribute
The `origin` attribute value to match on `<alt-trans>` elements. Trans-units without a matching alt-trans are excluded from the report. Only meaningful when "Use alt-trans" is enabled.

### Logging

#### Append the average results to a log
Appends average scores to a persistent tab-delimited UTF-8 log file with fields: UTC timestamp, base document path, second document path, ED-score, FM-score.

#### Log file
Full path to the log file.

## Limitations

- None known.

## Notes

- Neither score is very accurate on short segments. Place a Word Count Step before this step to include per-segment word counts in the report.
- Input documents must have the same number of text units in the same order.
- The Edit Effort Score is particularly useful in MT post-editing scenarios to quantify the effort needed to produce human-quality translations.

## Examples

### Compare human translation with MT output

```yaml
generateHTML: true
autoOpen: true
genericCodes: true
generateTMX: true
tmxPath: "${rootDir}/comparison.tmx"
target2Suffix: "-mt"
document1Label: "Human"
document2Label: "MT Output"
caseSensitive: true
whitespaceSensitive: false
punctuationSensitive: true
```

### Compare MT candidate within same XLIFF file

Use the same XLIFF file as both document 1 and document 2, with alt-trans enabled:

```yaml
useAltTrans: true
altTransOrigin: "BING"
generateHTML: true
document1Label: "MT Candidate"
document2Label: "Human Translation"
```

### Log file output format

```
2013-04-01 16:09:14+0000	/C:/myProject1/machine.tmx	/C:/myProject1/human.tmx	93.75	96.50
```
