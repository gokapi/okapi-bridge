# Regex Filter

The Regex Filter extracts translatable text from any text-based format using user-defined regular expressions with capturing groups. Each rule pairs a regex pattern with an action (extract content, extract strings, treat as comment, start/end section, or skip), and capturing groups map matched text to source, target, identifier, and note roles. The filter processes rules in order, scanning the document from top to bottom and applying the first matching rule at each position.

## Parameters

### Rules

#### Rules
Ordered list of extraction rules. Each rule has a Java regular expression, an action, and capturing group assignments for source, target, identifier, and note.

Rules are evaluated **in list order** — the first matching rule at each position wins. Order matters for correct extraction.

**Capturing groups** are numbered by opening parenthesis from left to right (group 0 = entire match). Assign groups to roles:
- **Source group** — the translatable text (required for extract actions)
- **Target group** — pre-existing translation (use `-1` if undefined)
- **Identifier group** — resource name/ID (use `-1` if undefined)
- **Note group** — translator note (use `-1` if undefined)

**Actions:**

| Action | Effect | Source | Target | Identifier | Note |
|--------|--------|--------|--------|------------|------|
| Extract the strings in source group | `TEXT_UNIT` per string found | Required — contains strings | Not used | Name for first TU (sequential suffix for others) | Note for each TU |
| Extract the content of source group | Single `TEXT_UNIT` | Required — source text | Target text | TU name | TU note |
| Treat as comment | Scan for loc directives | Required — scanned | Not used | Not used | Not used |
| Do not extract | Skip | Not used | Not used | Not used | Not used |
| Start a section | `START_GROUP` event | Not used | Not used | Section name | Section note |
| End a section | `END_GROUP` event | Not used | Not used | Not used | Not used |

#### Preserve White Spaces
Preserve all white spaces in extracted text. When disabled, consecutive whitespace (spaces, tabs, CR, LF) is collapsed to a single space and leading/trailing whitespace is trimmed.

#### Has Inline Codes
Enable conversion of text patterns into inline codes. Configure patterns via the Inline Codes Patterns editor.

#### Auto-Close Sections
Automatically close the previous section when a new one starts. When enabled, only define **Start a section** rules — do **not** define matching **End a section** rules. When disabled, every start must have a corresponding end.

### Regular Expression Options

These options apply globally to all rules. Override per-rule using `(?idmsux-idmsux)` inline flag syntax in the pattern.

#### Dot Matches Line-Feed
Makes the `.` operator match line-feed characters (equivalent to Java `Pattern.DOTALL`).

#### Multi-Line Mode
Makes `^` and `$` match at line terminators within the input, not just at the absolute start/end (equivalent to Java `Pattern.MULTILINE`).

#### Ignore Case
Makes matching case-insensitive — `abc` matches `Abc`, `ABC`, etc. (equivalent to Java `Pattern.CASE_INSENSITIVE`).

### Localization Directives

#### Use Localization Directives
Enables the filter to recognize localization directives. When disabled, all directives are ignored.

#### Extract Outside Directives
Extract translatable items outside the scope of localization directives. Only active when **Use Localization Directives** is enabled.

### Strings

These settings control the **Extract the strings in the source group** action.

#### Beginning of String
Character(s) marking the start of a string. Multiple characters define multiple alternative delimiters, positionally paired with end characters.

#### End of String
Character(s) marking the end of a string. Must have the same count as beginning characters, with each position corresponding to its pair.

#### Backslash Escape
Treat backslash-prefixed characters (e.g., `\"`) as escaped literals, not delimiters.

#### Double Character Escape
Treat doubled characters (e.g., `""`) as escaped literals, not delimiters.

### Content Type

#### Document MIME Type
The MIME type for the document. Affects how text is written back. Use `text/plain` for most cases.

## Limitations

- The **entire document is loaded into memory** to apply regular expressions — very large documents may cause memory issues.
- The **Extract strings outside the rules** option is not yet implemented.
- The filter does not recognize encoding declarations in the document and therefore cannot update them automatically.

## Notes

- Unicode BOM is auto-detected; otherwise the default encoding specified when opening the document is used.
- UTF-8 output includes a BOM only if the input was also UTF-8 and had a BOM.
- Output preserves the input's line-break style.
- FPRM-only `metaRule` parameters allow adding matched regex content as metadata on TextUnits (not accessible via UI).

## Examples

### Key-Value Extraction

Extract translatable values from `key=value` or `key:value` format:

**Input:**
```
[ID1]=Text for ID1
[ID2]:Text for ID2
```

**Rule:** `^\[(.*?)\](=|:)(.*?)$` with source group = 3, identifier group = 1, multi-line enabled.

**Output (XLIFF):**
```xml
<trans-unit id="1" resname="ID1" xml:space="preserve">
  <source xml:lang="en">Text for ID1</source>
</trans-unit>
<trans-unit id="2" resname="ID2" xml:space="preserve">
  <source xml:lang="en">Text for ID2</source>
</trans-unit>
```

### FPRM Metadata Rules

Add regex-matched content as metadata (FPRM only, not in UI):

```
metaRuleCount.i=2
metaRule0.ruleName=meta1
metaRule0.expr=(\d\d:\d\d:\d\d)
metaRule1.ruleName=meta2
metaRule1.expr=(\d\d:\d\d:\d\d)
```
