# Whitespace Correction

Simplifies the addition or removal of inter-segment whitespace when translating to or from Chinese or Japanese scripts that do not typically use spaces. When translating from a space-delimited language (e.g., English) to a non-space-delimited language (e.g., Chinese, Japanese), whitespace following segment-ending punctuation is removed. In the reverse direction, whitespace is added. No action is performed between two space-delimited languages or between Chinese and Japanese.

## Parameters

#### Punctuation Types

Comma-separated list of punctuation categories to apply whitespace correction to. Each category handles conversion between full-width CJK punctuation and their ASCII equivalents:

- **FULL_STOP** — Ideographic Full Stop (U+3002) and Full-width Full Stop (U+FF0E) ↔ period (`.`)
- **COMMA** — Ideographic Comma (U+3001) and Full-width Comma (U+FF0C) ↔ comma (`,`)
- **EXCLAMATION_MARK** — Full-width Exclamation Mark (U+FF01) ↔ `!`
- **QUESTION_MARK** — Full-width Question Mark (U+FF1F) ↔ `?`

The step both converts the punctuation characters and adjusts the trailing whitespace in one operation.

Default: `FULL_STOP,COMMA,EXCLAMATION_MARK,QUESTION_MARK`

#### Whitespace Characters

Comma-separated list of Unicode whitespace character categories that the step will add or remove when adjusting inter-segment spacing. The default includes a comprehensive set of Unicode space characters including regular spaces, non-breaking spaces, em/en spaces, ideographic spaces, and zero-width spaces.

Most users should not need to modify this parameter. Adjust only if you need to restrict which whitespace characters are considered during correction.

## Limitations

- Relies on the assumption that each source segment contains a single sentence and has been translated to a single sentence in the target language.
- No action is performed when both source and target are space-delimited languages (e.g., English to French).
- No action is performed when translating between Chinese and Japanese (both non-space-delimited).

## Notes

- The step converts full-width CJK punctuation to/from ASCII equivalents as part of the whitespace adjustment — it does not only add/remove spaces.
- Direction of correction (add vs. remove whitespace) is determined automatically from the source and target locale settings.

## Examples

### English to Japanese (remove whitespace)

When translating from English (space-delimited) to Japanese (non-space-delimited), trailing whitespace after segment-ending punctuation is removed and ASCII punctuation is converted to full-width equivalents.

### Japanese to English (add whitespace)

When translating from Japanese to English, the step adds whitespace after segment-ending punctuation and converts full-width punctuation to ASCII equivalents.
