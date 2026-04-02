# Cleanup Step

The Cleanup step cleans strings by normalizing quotes, punctuation, and whitespace ready for further processing. By default, all whitespace is normalized before any further processing — all multiple space, tab, etc. characters are replaced with a single instance. It operates on filter events and requires both source and target language to be set.

## Parameters

#### Normalize Quotation Marks
Replaces all quotation marks with straight double quotes (`"`) and all apostrophes with single straight quotes (`'`). This is useful for normalizing curly/smart quotes and other typographic quote variants into a consistent ASCII form before further processing.

#### Check for Corrupt or Unexpected Characters
Detects and removes text units that contain common corrupt character strings. This can catch encoding issues such as mojibake or other garbled text that would otherwise propagate through the pipeline.

#### Mark Segments Matching Default Regular Expressions for Removal
Intended to mark segments matching built-in regular expressions for removal.

> **Note:** This option is not currently used by the step. Enabling or disabling it has no effect.

#### Mark Segments Matching User Defined Regular Expressions for Removal
Removes text units that contain text matching a user-defined regular expression. Use this to filter out segments with specific patterns (e.g., placeholder-only strings, test data) before further processing.

#### Remove Unnecessary Segments from Text Unit
Removes text units that have been marked for removal by other checks in this step, or that have no target text. This is the final cleanup pass — segments flagged by the character check or regex matching are actually removed when this option is enabled.

#### Standardize Punctuation Spacing
Standardizes spaces before and after certain punctuation marks to conform to English writing style conventions. For example, ensures no space before periods or commas, and a single space after them.

> **Warning:** This applies English punctuation spacing rules, which may not be appropriate for all languages (e.g., French uses a space before `:`, `!`, `?`).

#### Keep CR and NL Characters Intact
Preserves carriage return (CR) and newline (NL) characters as-is during whitespace normalization. By default, the step normalizes all whitespace (including CR/NL) to ASCII spaces. Enable this to keep line breaks in the text while still normalizing spaces and tabs.

## Limitations

- Does not work with Asian languages.
- Does not work with bi-directional (RTL) languages.
- The `matchRegexExpressions` option is not currently implemented and has no effect.

## Notes

- All whitespace is normalized before any further processing — multiple spaces, tabs, etc. are replaced with a single space instance.
- CR and NL characters are also normalized to spaces by default unless `keepCrNlIntact` is enabled.
- Text units are only actually removed when `pruneTextUnit` is enabled, even if other options flag them for removal.
