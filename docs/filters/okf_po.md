# PO Filter

The PO Filter processes Gettext PO (Portable Object) and POT (PO Template) resource files. It supports both **bilingual mode** (where `msgid` is the source and `msgstr` is the translation) and **monolingual mode** (where `msgid` is a real identifier and `msgstr` is the text). The filter handles plural forms, domains, fuzzy flags, various comment types, and automatic encoding detection from the PO header's `charset` declaration.

## Parameters

### Processing Mode

#### Bilingual Mode
In **bilingual mode**, `msgid` is the source text and `msgstr` is the translation — this is the standard PO file layout.

In **monolingual mode** (when disabled), `msgid` is treated as a real identifier (not source text), and `msgstr` contains the translatable text. The `msgid` value becomes the resource name.

Most PO files are bilingual. Use monolingual mode only when your PO workflow uses `msgid` as a key/identifier rather than human-readable source text.

#### Protect Approved Entries
Marks all entries that are **not empty and not fuzzy** with a non-translatable flag, preventing them from being modified during translation.

This is useful when you want to re-process a PO file but only allow changes to entries that are still fuzzy or untranslated.

> **Note:** The fuzzy flag (`#, fuzzy`) in PO files indicates a proposed translation. Entries without this flag and with non-empty `msgstr` are considered approved.

#### Wrap Content
Controls whether long content lines in `msgid` and `msgstr` entries are wrapped across multiple quoted strings in the output. PO files conventionally wrap long strings with an empty first line followed by continuation lines.

### Identification

#### Generate Text Unit IDs
Generates identifiers from the hash code of the source text (`msgid`), optionally combined with the domain prefix and `msgctxt` value. Only meaningful when bilingual mode is enabled.

Two entries with the same source text but different `msgctxt` values will get different identifiers. If `msgctxt` uses the Okapi format `okpCtx:tu=123`, the text unit ID is set to the specified value.

> **Note:** The generated ID may not be unique if the source text is identical within the same domain and has no distinct context values.

#### Include Message Context in Notes
Includes the content of the `msgctxt` line as a localization note attached to the extracted text unit. This makes the context visible to translators in tools that display notes.

### Output

#### Output in Generic Format
Outputs the PO file in a generic (normalized) format rather than preserving the original formatting style.

#### Allow Empty Output Target
Allows writing empty `msgstr` entries in the output when no translation is available.

### Inline Codes

#### Enable Inline Code Detection
Enables pattern-based detection of inline codes (placeholders, escape sequences, etc.) within translatable text. Matched patterns are converted to inline code placeholders.

The default patterns detect:
- **printf-style placeholders**: `%s`, `%d`, `%2$s`, `%-10.2f`
- **Escape sequences**: `\n`, `\r`, `\t`, `\a`, `\b`, `\f`, `\v`, `\r\n`
- **Numbered placeholders**: `{0}`, `{1,date}`, etc.

#### Inline Code Rules
Regex patterns that identify inline codes. Each pattern is a Java regular expression. The default rule set:
```
((%(([-0+#]?)[-0+#]?)((\d\$)?)([\d\*]*(\.[\d\*]*)?)[dioxXucsfeEgGpn])
|((\\r\\n)|\\a|\\b|\\f|\\n|\\r|\\t|\\v)
|(\{\d.*?\}))
```

#### Merge Adjacent Codes
Merges consecutive inline codes with no translatable text between them into a single placeholder.

#### Move Boundary Codes
Moves inline codes at segment boundaries to the skeleton (non-translatable), removing them from translator view.

## Limitations

None known.

## Notes

- **Encoding detection**: BOM → `charset` in first 1000 chars → default encoding. BOM wins if there's a conflict.
- The header `charset` declaration is automatically updated to match the output encoding.
- **No language information** is updated in the PO header — update manually.
- Plural forms (`msgstr[0]`, `msgstr[1]`, ...) must appear in sequential order and are grouped as `x-gettext-plurals`.
- Domains are represented as groups with type `x-gettext-domain`.
- The **fuzzy flag** (`#, fuzzy`) maps to the `approved` target property.
- Comment types: extracted (`#.` → note), translator (`# ` → transnote), reference (`#:` → references). Context comments (`#|`) are ignored.
- The `msgctxt` value differentiates generated identifiers; the Okapi format `okpCtx:tu=NNN` directly sets the text unit ID.

## Examples

### Standard Bilingual PO File
```po
msgid \"diverging after version %d of %s\"
msgstr \"\"

msgid \"You have selected %d file for deletion\"
msgid_plural \"You have selected %d files for deletion\"
msgstr[0] \"\"
msgstr[1] \"\"
```

### Monolingual Configuration
```json
{
  \"mode\": { \"bilingualMode\": false },
  \"identification\": { \"makeID\": true }
}
```"
