# XML Stream Filter

The XML Stream Filter processes XML documents using a stream parser, allowing it to handle much larger documents than the DOM-based XML Filter. It uses the same YAML-based configuration syntax as the HTML Filter, with all HTML Filter parameters available except `escapeCharacters`. If you need ITS (Internationalization Tag Set) support, use the XML Filter instead.

## Parameters

### Parser

#### Assume Well-Formed
Controls whether the parser assumes the input XML is well-formed. When enabled, the parser uses a more lenient parsing mode. The default configurations all set this to `true`.

#### Preserve Whitespace
Controls the global default for whitespace preservation. Individual elements can override this via `PRESERVE_WHITESPACE` element rules (e.g., `<pre>`, `<codeblock>`) or the `xml:space` attribute. When `false` (the default), insignificant whitespace is normalized.

### Inline Codes

#### Enabled
Activates pattern-based detection of inline codes (placeholders, markup tags, variables, etc.) within translatable text. The patterns are defined in the rules section.

#### Rules
Regex patterns that identify inline codes within translatable text segments. Each pattern should match a complete inline code. These are Java regex patterns applied to the extracted text content.

#### Merge Adjacent
When multiple inline codes appear next to each other with no translatable text between them, merge them into a single code placeholder. This simplifies the segments presented to translators.

#### Move Boundary Codes
Moves inline codes that appear at the very start or end of a segment out into the non-translatable skeleton. This keeps segment boundaries clean for translation tools.

#### Simplifier Rules
Rules for simplifying the representation of inline codes in extracted segments.

### Attributes
Defines global attribute extraction rules that apply across all elements. Each key is an attribute name mapped to a rule specifying how that attribute should be handled.

The default configuration handles:
- `xml:lang` — writable localizable (language tag)
- `xml:id` and `id` — used as segment identifiers
- `xml:space` — controls whitespace preservation state

### Other Parameters

#### Editor Title
Title displayed in the parameters editor UI.

#### Tagged Configuration
Internal parsed configuration object for element and attribute rules.

#### Configuration File Path
Path to an external YAML configuration file.

## CDATA Subfiltering

The `global_cdata_subfilter` parameter specifies a secondary filter applied to all CDATA content. For example, to process CDATA as HTML:

```yaml
global_cdata_subfilter: okf_html
```

## PCDATA Subfiltering

The `global_pcdata_subfilter` parameter applies a secondary filter to escaped markup in PCDATA content. Only content matched by `TEXTUNIT` element rules is passed to the subfilter — `INCLUDE` rules are **not** subfiltered.

```yaml
global_pcdata_subfilter: okf_html
elements:
  test:
    ruleTypes: [TEXTUNIT]
```

## Quote Mode

Escaping of quote and apostrophe characters can be configured:

```yaml
quoteModeDefined: true
quoteMode: 3
```

- **0** — Do not escape single or double quotes
- **1** — Escape single and double quotes to named entities
- **2** — Escape double quotes to named entities, single quotes to numeric entities
- **3** — Escape double quotes only

## Limitations

- No transparent support for namespace prefixes — element names must be declared with their full prefix in the configuration.
- The filter is **not case-sensitive** for element names — `<elem>` and `<Elem>` are treated as identical, violating the XML specification.
- Does not support ITS — use the XML Filter instead.
- The `escapeCharacters` parameter from the HTML Filter is not available.

## Notes

- **Input encoding**: Uses the document's encoding declaration if present; defaults to UTF-8 otherwise.
- **Output encoding**: BOM is preserved only if the input was UTF-8 with a BOM. XML encoding declarations are updated or added automatically.
- **Line breaks**: Output preserves the input document's line-break style.

## Examples

### Java Properties XML with Embedded HTML

```yaml
assumeWellformed: true
global_cdata_subfilter: okf_html
preserve_whitespace: false

elements:
  entry:
    ruleTypes: [TEXTUNIT]
    idAttributes: [key]
```

### PCDATA Subfiltering for Escaped HTML

Input:
```xml
<test>
  &lt;p&gt;This is embedded HTML content.&lt;/p&gt;
</test>
```

Config:
```yaml
global_pcdata_subfilter: okf_html
elements:
  test:
    ruleTypes: [TEXTUNIT]
```
