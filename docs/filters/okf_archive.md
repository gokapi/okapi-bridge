# Archive Filter

The Archive Filter processes files stored inside ZIP archive files (e.g. `.zip` or `.jar` files). It extracts files matching specified name patterns from the archive and processes each one using a designated Okapi filter configuration. This enables localization of translatable content embedded within archive formats.

## Parameters

### Identification

#### MIME Type of the Filter's Container Format

The MIME type identifying the **archive container** format itself, not the files inside it. The default `application/x-archive` is appropriate for standard ZIP files. Only change this if you are processing a specialized archive format that requires a different MIME type.

#### File Names

Comma-separated list of file name patterns to match against entries **inside** the archive. Files matching any pattern will be extracted and processed with the corresponding filter.

- Patterns match file names in **any directory** within the archive
- Supports wildcard characters: `?` (single character) and `*` (any characters)
- The number of patterns must match the number of entries in **Filter configuration ids**

Examples:
- `Res.properties` — matches files named exactly `Res.properties`
- `*.tmx,*.xlf` — matches all TMX and XLIFF files

#### Filter Configuration IDs

Comma-separated list of Okapi filter configuration identifiers, one for each file name pattern.

- Must have **exactly as many entries** as file name patterns, in the **same order**
- Each config ID specifies which filter processes the files matched by the corresponding pattern
- Use default filter IDs (e.g. `okf_properties`, `okf_html`) for standard configurations

### Inline Codes

Standard inline code detection settings. Enable pattern-based detection of inline codes (placeholders, tags, etc.) within extracted text.

## Limitations

- None known.

## Notes

- Files inside the archive are matched by name pattern against entries in any directory within the ZIP structure.
- Each file name pattern is paired positionally with a filter configuration ID.

## Examples

### Extract properties files from a JAR

Process all `.properties` files inside a JAR archive:

```yaml
identification:
  fileNames: \"*.properties\"
  configIds: \"okf_properties\"
  mimeType: \"application/x-archive\"
```

### Multiple file types in a ZIP

Extract and process both TMX and XLIFF files:

```yaml
identification:
  fileNames: \"*.tmx,*.xlf,*.xlff\"
  configIds: \"okf_tmx,okf_xliff,okf_xliff\"
  mimeType: \"application/x-archive\"
```"
