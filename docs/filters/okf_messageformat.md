# MessageFormat Filter

The MessageFormat Filter handles message formats commonly found in software applications, including **ICU Message Format** and **Java MessageFormat**. It is designed to be used as a **sub-filter** — the message strings can reside in various container formats such as JSON, YAML, or XML. The filter parses complex message syntax (`plural`, `select`, etc.) to expose translatable content.

## Parameters

#### Add Plural Forms
Adds missing plural forms to the source text based on the **target locale's** CLDR plural rules. The source string is modified and re-filtered so that all required plural categories (e.g., `zero`, `one`, `two`, `few`, `many`, `other`) are presented to the translator.

This is useful when the source language (e.g., English) has fewer plural forms than the target language (e.g., Arabic with 6 forms). Without this option, translators must manually add the missing categories.

> **Note:** The source string is modified and then refiltered — the translator sees plural forms that may not exist in the original source.

Default: `false`

#### Normalize Variants
Moves leading and trailing text that appears outside complex argument variants (`plural`, `select`, etc.) **into each variant**. This forces each variant to be a complete, self-contained phrase or sentence, making translation easier.

For example, `Hello {count, plural, one {# item} other {# items}} found` becomes variants like `Hello # item found` and `Hello # items found`.

> **Warning:** This option can increase word and character counts for the source, since the shared leading/trailing text is duplicated into every variant.

Default: `false`

#### Pretty Print
Formats the output with indentation and line breaks for readability. Any whitespace added is **not significant** — it does not affect the meaning of the message. When disabled, the output is a compact string, normally on a single line.

Default: `false`

## Limitations

- `choice` syntax is not supported (e.g., `{count, choice, 0#is none|1#is one|1<is more than one}`). This syntax is deprecated and should be converted to `select` or `plural`.

## Notes

- This filter is a **sub-filter** and must be invoked from a container format filter (JSON, YAML, XML, etc.).
- Handles ICU Message Format and Java MessageFormat syntax including `plural`, `select`, and `selectordinal` arguments.

## Examples

### Normalization effect

Shows how `normalize` moves surrounding text into each plural variant to create complete sentences.

**Input:**
```
Hello {count, plural, one {# item} other {# items}} found
```

**Output (normalized):**
```
{count, plural, one {Hello # item found} other {Hello # items found}}
```
