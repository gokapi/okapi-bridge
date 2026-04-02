# DTD Filter

This filter processes DTD (Document Type Definition) documents containing translatable text entity declarations. It is primarily intended for XML-DTD files like Mozilla DTD files used with XUL documents for user-interface localization. The filter extracts text from `<!ENTITY>` declarations for translation.

## Parameters

### useCodeFinder

Enables the inline code finder, which uses regular expressions to identify spans of extracted text that should be treated as inline codes rather than translatable content.

Useful when entity values contain variables or placeholders (e.g. `VAR1`) that must be protected from modification during translation.

### codeFinderRules

Regular expression rules in Okapi `#v1` format that define patterns to match as inline codes within extracted text.

The format starts with `#v1`, followed by `count.i=N` (number of rules), then `rule0=...`, `rule1=...`, etc.

**Important:** When writing rules in YAML configuration, backslashes must be double-escaped (e.g., `\b` for a word boundary ``).

**Example:**

```yaml
useCodeFinder: true
codeFinderRules: "#v1
count.i=1
rule0=\bVAR\d\b"
```

This configuration marks `VAR1` as an inline code in:

```xml
<!ENTITY dialog.fileCount "Number of files = VAR1">
```

## Processing Notes

- Input encoding is determined by BOM if present; otherwise falls back to the default encoding specified in filter options.
- UTF-8 output includes a BOM only if the input was UTF-8 and had a BOM; otherwise no BOM is written.
- Output line-break style matches the original input document.

## Limitations

None known.

## Examples

### Mozilla DTD with variable placeholders

Input DTD:

```xml
<!--Comments-->
<!ENTITY findWindow.title "Find Files">
<!ENTITY fileMenu.label "File">
<!ENTITY editMenu.label "Edit">
<!ENTITY dialog.fileCount "Number of files = VAR1">
```

Configuration to protect variables as inline codes:

```yaml
useCodeFinder: true
codeFinderRules: "#v1
count.i=1
rule0=\bVAR\d\b"
```
