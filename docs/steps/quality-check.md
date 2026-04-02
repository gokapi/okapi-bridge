# Quality Check Step

The Quality Check step compares source and target text units to generate a report of potential translation issues. It supports both filter events and raw document input, performing checks on whitespace, empty segments, inline codes, character patterns, length ratios, terminology, and more. When used with filter events, the report is generated at batch end; with raw documents, the session opens in CheckMate for interactive review.

No checks are performed on entries flagged as non-translatable.

## Parameters

### Report Settings

#### Report Output Path
Path of the HTML report file. Supports `${rootDir}` variable for the input root directory.

#### Report Output Type
Format of the generated report.

#### Open Report After Completion
Automatically opens the report in the default browser after processing.

#### Show Full File Path
Displays full file paths in the report instead of filenames only.

#### Save Session
Saves the QA session to a `.qcs` file for later review in CheckMate.

#### Session File Path
Path for the saved session file. Supports `${rootDir}`.

### Text Unit Checks

#### Check Leading Whitespace
Flags text units where leading whitespace differs between source and target.

#### Check Trailing Whitespace
Flags text units where trailing whitespace differs between source and target.

### Segment Checks

#### Check Empty Target
Flags segments where the target is empty but the source has content.

#### Check Empty Source
Flags segments where the source is empty but the target has content.

#### Check Target Same as Source
Flags segments where target is identical to source. Only applies when the source contains at least one word character (`[\p{Ll}\p{Lu}\p{Lt}\p{Lo}\p{Nd}]`). Inline codes are excluded from comparison.

> **Note:** When a target-same-as-source is detected, pattern rules with `<same>` as expected target are cross-referenced. If the text matches such a pattern, no warning is generated.

#### Include Codes in Comparison
Includes inline codes in the identity comparison. If the only difference is a code, the segment is not flagged.

#### Include Numbers in Comparison
Includes numeric values in the identity comparison.

#### Check Doubled Words
Flags consecutive repeated words in the target (case-insensitive). Configure exceptions with a semicolon-separated list (no spaces around delimiters).

### Inline Code Checks

#### Check Code Differences
Verifies source and target have the same inline codes. Reports missing and extra codes. Order differences alone do not trigger warnings unless strict order is enabled.

#### Strict Code Order
Also flags code reordering, not just missing/extra codes.

#### Guess Open/Close Codes
Infers opening/closing status of codes when not explicitly marked.

#### Extra Codes Allowed / Missing Codes Allowed
Number of codes allowed to differ without triggering a warning.

#### Code Types to Ignore
Semicolon-separated code types to skip (e.g., `mrk;x-df-s;`).

### Length Checks

#### Check Maximum Length
Flags targets longer than a percentage of source length. Uses separate thresholds for short and long text based on a character-count breakpoint. Length excludes inline codes and should be tuned per language pair.

#### Check Minimum Length
Same logic as maximum length but for targets that are too short.

#### Check Absolute Max Length
Flags any target exceeding a fixed character count regardless of source length.

#### Check Storage Size
Verifies target fits within format-defined storage limits (e.g., XLIFF `maxwidth`).

### Character Checks

#### Check Corrupted Characters
Detects patterns indicating encoding corruption (e.g., UTF-8 opened as ISO-8859-1). Only catches common patterns.

#### Check Against Charset
Validates target characters against a specified encoding (e.g., `ISO-8859-1`). Characters not representable are flagged. Use `extraCharsAllowed` regex to whitelist additional characters.

### Pattern Checks

Up to 8 pattern rules (0–7) can be defined. Each rule has:
- **Enable** — toggle the rule on/off
- **Direction** — source→target (`true`) or target→source (`false`, for detecting extras)
- **Single Match** — expect only one occurrence
- **Severity** — 0 (low), 1 (medium), 2 (high)
- **Source/Target Regex** — Java regular expressions; use `<same>` in target to expect identical content
- **Description** — shown in the report

Default patterns check: parentheses, brackets, email addresses, URLs, IP addresses, C-style printf codes, and tripled letters.

### Terminology

#### Check Terminology
Verifies terms from a glossary file are correctly translated. Use `stringMode` for substring matching and `betweenCodes` to restrict to text between inline codes.

### Blacklist

#### Check Blacklist
Checks target (and optionally source) against a file of forbidden terms. `allowBlacklistSub` enables substring matching.

### LanguageTool Integration

#### Check with LanguageTool
Runs grammar/style checks via a LanguageTool server. Requires a running instance at the configured URL.

> **Warning:** LanguageTool may significantly increase processing time.

### Processing Scope

Controls which entries are checked: all entries, approved only, or non-approved only. Non-translatable entries are always skipped.

### XLIFF Validation

#### Validate XLIFF Schema
Validates input against Transitional or Strict XLIFF schema.

## Limitations

- No checks on non-translatable entries.
- Corrupted character detection only catches common encoding patterns.
- LanguageTool integration significantly increases processing time.
- Length checks exclude inline codes and must be tuned per language pair.

## Notes

- With filter events input, events pass through; report generates at batch end.
- With raw documents, a CheckMate session opens for interactive review.
- A *text unit* contains one or more *segments*; whitespace checks are at text-unit level, most other checks at segment level.
- This step shares its configuration module with CheckMate.

## Examples

### Basic QA with default patterns
```yaml
outputPath: ${rootDir}/qa-report.html
autoOpen: true
checkPatterns: true
codeDifference: true
doubledWord: true
```

### Length-focused QA for UI strings
```yaml
checkMaxCharLength: true
maxCharLengthBreak: 30
maxCharLengthAbove: 50
maxCharLengthBelow: 20
checkAbsoluteMaxCharLength: true
absoluteMaxCharLength: 128
```
