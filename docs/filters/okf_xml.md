# XML Filter

The XML Filter processes XML documents using a DOM-based parser with full **ITS (Internationalization Tag Set)** support (both 1.0 and 2.0). By default, all element content is translatable and no attribute values are translatable — behavior is customized via ITS rules in a parameters file (`.fprm`). If you need to process very large XML documents and have no need for ITS, consider using the **XML Stream Filter** instead.

## Parameters

This filter stores its parameters in an ITS XML document (`.fprm` file). There is no graphical editor — edit the file with a text or XML editor.

### ITS Support

By default the filter processes XML documents based on **ITS defaults**:
- The content of all elements is translatable
- None of the attribute values are translatable

Behavior is customized through ITS rules which are applied in this order (later rules override earlier):
1. External parameters file (`.fprm`) global rules
2. Document-embedded global rules
3. Document local rules

The filter supports ITS 1.0 and ITS 2.0 (backward compatible).

### ITS Extensions

Extensions use the namespace `http://www.w3.org/2008/12/its-extensions`.

#### idValue and xml:id

> **Note:** This extension was defined for ITS 1.0. ITS 2.0 offers the new **Id Value** data category that should be used instead.

When `xml:id` is found on a translatable element, it is used as the text unit name. The `itsx:idValue` attribute in `translateRule` allows defining an XPath expression for the identifier. `xml:id` takes precedence over `idValue`.

#### whiteSpaces

> **Note:** This extension was defined for ITS 1.0. ITS 2.0 offers the new **Preserve Space** data category that should be used instead.

The `itsx:whiteSpaces="preserve"` attribute applies the equivalent of `xml:space="preserve"` globally via ITS rules. The `xml:space` attribute takes precedence.

### Filter Options

Filter options use the namespace `okapi-framework:xmlfilter-options`. **All options must be placed in the parameters file (`.fprm`), not in embedded or linked ITS rules.** Multiple options must be in a single `<okp:options>` element.

#### lineBreakAsCode

Treats line-break characters (`&#10;`) in element content as inline codes rather than literal line breaks. Useful for Excel-generated XML where `&#10;` entity references mark cell formatting. Affects all extracted content globally.

Default: `no`

#### codeFinder

Defines regular expressions to capture spans of extracted text that should be treated as inline codes. Uses a specific `#v1` serialization format. Set `useCodeFinder="yes"` to activate. Patterns must be XML-escaped.

> **Note:** Regex patterns must be escaped for XML — `<(/?)\w[^>]*?>` becomes `&lt;(/?)\w[^&lt;]*?&gt;`.

#### omitXMLDeclaration

Suppresses the XML declaration from the output. By default a declaration is always written.

Default: `no`

> **Warning:** XML documents without a declaration may be read incorrectly if the encoding is not UTF-8, UTF-16, or UTF-32.

#### escapeQuotes

Controls whether double-quote characters are escaped in element content. Only takes effect if no ITS rule triggers translation of any attribute in the document.

Default: `yes`

#### escapeGT

Controls whether `>` is escaped as `&gt;` in the output.

Default: `yes`

#### escapeNbsp

Controls whether the non-breaking space character is escaped as `&#x00a0;`.

Default: `yes`

#### extractIfOnlyCodes

Controls whether entries containing only whitespace and/or inline codes are extracted.

Default: `yes`

#### inlineCdata

When enabled, CDATA markers are exposed as inline codes instead of being discarded.

Default: `no`

#### extractUntranslatable

Extracts entries marked `its:translate="no"` for context. Extracted entries are marked `translate="no"` in XLIFF output. Use `localeFilterList="!*"` to selectively exclude specific elements.

Default: `no`

## Limitations

- The ITS rule `withinTextRule` with value `nested` may act like `yes` in some cases.
- `xml:lang` attribute values are **not** updated in output to reflect the target language.
- The entire input file is loaded into memory (DOM parser) — very large documents may cause memory issues.

## Notes

- **Input encoding**: Uses the document's encoding declaration if present; otherwise defaults to UTF-8.
- **Output encoding (UTF-8)**: BOM is written only if input was UTF-8 and had a BOM.
- **XML declaration**: Automatically added or updated unless `omitXMLDeclaration` is set.
- **Line-breaks**: Output preserves the same line-break style as the input.
- **ITS rule precedence**: External parameters → document global → document local (later overrides earlier).

## Examples

### Basic ITS Parameters File

```xml
<its:rules version="1.0"
 xmlns:its="http://www.w3.org/2005/11/its"
 xmlns:okp="okapi-framework:xmlfilter-options">
 <its:translateRule selector="//head" translate="no"/>
 <its:withinTextRule selector="//b|//code|//img" withinText="yes"/>
 <okp:options lineBreakAsCode="yes" escapeQuotes="no"/>
</its:rules>
```

### Code Finder with Regex Patterns

```xml
<okp:codeFinder useCodeFinder="yes">#v1
count.i=2
rule0=&lt;(/?)\w[^&lt;]*?&gt;
rule1=(#\w+?\#)|(%\d+?%)
</okp:codeFinder>
```

### Custom ID Values

```xml
<its:translateRule selector="//text" translate="yes" itsx:idValue="concat(../@name, '_t')"/>
<its:translateRule selector="//desc" translate="yes" itsx:idValue="concat(../@name, '_d')"/>
```
