# Tokenization Step

The Tokenization step creates annotations containing different tokenized forms of text units in a document. It recognizes 18+ token types (words, numbers, URLs, emails, emoji, CJK characters, etc.) using Unicode-aware and language-specific rules. Tokenization annotations are attached to filter events and can be consumed by downstream pipeline steps or external tools.

Takes: Filter events. Sends: Filter events.

## Parameters

### Scope

#### Tokenize Source
Tokenizes the content of the **source** locale. At least one of `tokenizeSource` or `tokenizeTargets` should be enabled for the step to produce output.

#### Tokenize Targets
Tokenizes the content of all **target** locales. Useful when you need token-level analysis of translated content for quality checks or alignment.

### Token Types

All token type booleans default to `false`. If no token types are selected, all available token types are generated.

#### WORD
Matches runs of characters that constitute a word in the given language. Word boundaries are determined by the locale's tokenizer rules, so results vary by language.

#### HYPHENATED_WORD
Matches words that include hyphens of various types (e.g., `well-known`, `state-of-the-art`). Captures the entire hyphenated compound as a single token.

#### NUMBER
Matches numeric values including those with comma or period separators (e.g., `1,234.56`, `3.14`).

#### PUNCTUATION
Matches punctuation characters as classified by Unicode general category `P`.

#### WHITESPACE
Matches whitespace characters as classified by Unicode — spaces, tabs, line breaks, and other separator characters.

#### ABBREVIATION
Matches a limited set of English-style abbreviations such as `pct` in `3.3pct`, `U.S.`, and `USD`.

> **Note:** Recognition is limited to English abbreviation patterns.

#### INTERNET
Matches Internet addresses including URIs and IP addresses (e.g., `http://www.somesite.org/foo/index.html`, `192.168.0.5`).

#### EMAIL
Matches e-mail address patterns (e.g., `user@example.com`).

#### EMOTICON
Matches text-based emoticon sequences like `:-)`.

#### EMOJI
Matches all emoji characters as defined in the Unicode standard, including multi-codepoint sequences.

#### DATE
Matches dates in `MM/DD/YYYY` format only.

> **Note:** Only US-style `MM/DD/YYYY` dates are recognized. International date formats are not matched.

#### CURRENCY
Matches monetary sums. The wiki documentation mentions this is limited to US dollars, though coverage may vary by Okapi version.

#### TIME
Matches time expressions separated by `:` or `.` in both 24-hour and 12-hour (AM/PM) formats.

#### MARKUP
Matches runs of text that begin with `<` and end with `>`, capturing HTML/XML-style tags as single tokens.

#### OTHER_SYMBOL
Matches Unicode symbols — mathematical operators, technical symbols, and other miscellaneous symbols not covered by other token types.

#### KANA
Matches Japanese Hiragana and Katakana character runs.

#### IDEOGRAM
Matches ideographic characters as defined by Unicode — primarily CJK unified ideographs.

## Limitations

- Only the `MM/DD/YYYY` date format is recognized by the DATE token type.
- ABBREVIATION recognition is limited to English-style patterns.
- The wiki mentions COMPANY and STOPWORD token types not exposed in the current schema.

## Notes

- Token annotations are attached to the filter events and passed downstream — they do not modify the text content.
- The step requires both source and target languages to be configured in the pipeline.
- The wiki documents a locale filter parameter for fine-grained control over which locales are tokenized, not present in the current schema.

## Examples

### Extract words and numbers from source
```yaml
tokenizeSource: true
tokenizeTargets: false
WORD: true
NUMBER: true
```

### CJK-focused tokenization
```yaml
tokenizeSource: true
WORD: true
KANA: true
IDEOGRAM: true
```
