# Doxygen Filter

The Doxygen Filter extracts [Doxygen](http://www.stack.nl/~dimitri/doxygen/)-style comments from source code for translation. It supports C++-style (`///`), Javadoc-style (`/**`), Qt-style (`/*!`), and Python-style (`'''` or `"""`) comment blocks. Doxygen special commands, HTML commands, and XML commands are recognized and converted to inline codes. The filter preserves line numbers so that source and translated line numbers maintain a one-to-one correspondence.

## Processing Details

- **Encoding detection**: Uses Unicode BOM if present (UTF-8, UTF-16, etc.); otherwise falls back to the default encoding specified when opening the document.
- **Inline codes**: The full set of Doxygen special commands, HTML commands, and XML commands are recognized and interpreted, converting them to inline codes in extracted text units.
- **Line number preservation**: The filter maintains a one-to-one correspondence between source and translated line numbers.

## Parameters

### doxygen_commands

Defines how known Doxygen special commands are handled during extraction. Pre-populated with the full set of Doxygen special commands.

Each entry is keyed by command name (without prefix/suffix, case-sensitive) and supports:
- `type` — `PLACEHOLDER`, `OPENING`, or `CLOSING`
- `inline` — `true` for inline, `false` for block-level (default: `false`)
- `pair` — paired command name (e.g., `code` pairs with `endcode`)
- `translatable` — whether the block content is translatable (default: `true`)
- `parameters` — ordered list of parameter descriptors, each with:
  - `name` — organizational label (not used by filter)
  - `length` — `WORD`, `LINE`, `PHRASE`, or `PARAGRAPH`
  - `required` — affects how aggressively text is parsed as a parameter (default: `true`)
  - `translatable` — whether the parameter is translatable (default: `true`)

> **Note:** The `parameters` listing is optional. When present, parameters must be listed in the order they appear after the command.

> **Note:** Parameters with non-whitespace delimiters (e.g. `.py` in `