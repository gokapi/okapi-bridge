# TXML Filter

The TXML Filter processes **Wordfast Pro TXML documents**, a proprietary XML-based bilingual format used by Wordfast Pro and supported by several other tools. It extracts blocks of one or more segments as text units, preserving existing translations and handling revisions. There are no official public specifications for the TXML format.

## Parameters

### Allow Empty Target Segments

Controls what happens when a translated segment has an empty target:

- **Enabled**: Empty translations are written as-is to the output TXML file
- **Disabled**: A copy of the source text is used in place of the empty translation

This is useful when downstream tools require non-empty target segments, or when you want to preserve empty targets as a signal that translation is still needed.

## Processing Details

### Encoding

- Uses the XML encoding declaration if present; otherwise defaults to **UTF-8** regardless of any externally specified default encoding.
- On output, if the original document had an XML encoding declaration it is updated; if it did not, one is automatically added.
- **BOM handling**: When output is UTF-8 and input was also UTF-8, a BOM is written only if one was detected in the input. Converting from another encoding to UTF-8 produces no BOM.

### Line-Breaks

Output line-break style matches the original input document.

### Segmentation

TXML files are always segmented — each block contains one or more segments. The content of each block is extracted as a single text unit with all its segments.

### Existing Translations

Translated segments are extracted with both source and target. Segments marked with `gtmt="true"` (without `modified="true"`) also generate an `AltTranslationsAnnotation`.

### Revisions

Only the latest translation is extracted; content in `<revisions>` elements is ignored. On merge, updated translations overwrite originals — no revision entries are created in the output TXML document.

## Limitations

- The TXML format does not allow different source and target content for inter-segment parts. If these parts change after extraction, only the source representation is preserved on merge — the difference cannot be represented.

## Examples

### Machine Translation Annotation

When a TXML segment has `gtmt="true"` but not `modified="true"`, the filter creates an `AltTranslationsAnnotation` alongside the normal source/target extraction:

```xml
<segment gtmt="true">
  <source>Hello world</source>
  <target>Bonjour le monde</target>
</segment>
```

This allows downstream pipeline steps to distinguish machine-translated content from human-reviewed translations.
