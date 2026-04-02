# HTML5-ITS Filter

Processes **HTML5** documents, including those containing **ITS 2.0** (Internationalization Tag Set) markup. Input documents are expected to be valid HTML5. The filter supports global and local ITS rules and most ITS 2.0 data categories, enabling fine-grained control over translatable content, terminology, language information, and directionality.

## Parameters

### ITS Parameters File (`path`)

Path to an **ITS rules file** that overrides the default HTML5 processing behavior.

The default behavior includes:
- `lang` attribute → Language Information
- `id` attribute → Id Value
- Phrasing content elements treated as inline (`withinText="yes"`)
- `translate` attribute → Translate data category (using HTML5-specific behavior, not XML)

Provide a custom ITS document to override any of these defaults.

### Simplifier Rules (`simplifierRules`)

Rules that control how inline codes are simplified for translation. These rules map HTML inline elements to shorter placeholder representations, making segments easier to translate.

### Move Boundary Codes to Skeleton (`moveLeadingAndTrailingCodesToSkeleton`)

Inline codes (e.g., `<b>`, `<span>`) that appear at the very start or end of a segment are moved out of the translatable content and into the skeleton. This simplifies segments for translators when boundary tags don't affect the translatable text.

### Merge Adjacent Codes (`mergeAdjacentCodes`)

When multiple inline codes appear next to each other with no text between them (e.g., `</b><i>`), they are combined into a single placeholder code. This reduces visual clutter in segments presented to translators.

## Quote Mode

Escaping of quote and apostrophe characters can be configured via config file properties:

| Mode | Value | Behavior |
|------|-------|----------|
| `0` | UNESCAPED | Do not escape single or double quotes |
| `1` | ALL | Escape both to named entities |
| `2` | NUMERIC_SINGLE_QUOTES | Named entity for double quotes, numeric for single |
| `3` | DOUBLE_QUOTES_ONLY | Escape double quotes only |

## ITS Support

The filter processes HTML5 documents based on ITS defaults:

- The `lang` attribute maps to the **Language Information** data category
- The `id` attribute maps to the **Id Value** data category
- Phrasing content elements are treated as inline (`withinText="yes"`)
- The `translate` attribute maps to the **Translate** data category (HTML5-specific behavior)

Default behavior can be overridden by ITS markup in the input document or by specifying a custom ITS parameters file.

The filter supports ITS 2.0 — see the [W3C ITS 2.0 specification](http://www.w3.org/TR/its20/) for details.

## Limitations

- This filter is **BETA**.

## Notes

- Input encoding is auto-detected per the HTML5 specification
- Output encoding matches input unless overridden by the calling tool
- Line-breaks in output match the original input style
- HTML5 `translate` attribute behavior differs from XML ITS — see the [HTML5 spec](http://www.w3.org/TR/2013/CR-html5-20130806/dom.html#the-translate-attribute)

## Examples

### Default ITS Rules

The default configuration defines standard HTML5 behavior:

```xml
<its:rules xmlns:its="http://www.w3.org/2005/11/its" version="2.0"
 xmlns:h="http://www.w3.org/1999/xhtml">
  <its:translateRule selector="//h:script|//h:style" translate="no"/>
  <its:translateRule selector="//h:*/@alt|//h:*/@title" translate="yes"/>
  <its:translateRule selector="//h:meta[@name='keywords']/@content" translate="yes"/>
  <its:preserveSpaceRule selector="//h:pre|//h:textarea" space="preserve"/>
</its:rules>
```

### Quote Mode Configuration

```properties
quoteModeDefined=true
quoteMode=3
```
