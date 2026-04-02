# Search and Replace Step

This step performs search and replace actions on either the text units or the full content of input documents. It can process both raw documents (whole-file text replacement) and filter events (text unit content replacement). Patterns are processed in the order they are declared, and both literal and Java regular expression modes are supported.

## Input Modes

- **Filter events**: Search and replace operates on the content of text units. Updated filter events are sent to the next step.
- **Raw document**: Search and replace operates on the whole file as plain text. The document is treated exactly as it would appear in a text editor — no conversion of escaped characters is performed. An updated raw document is sent to the next step.

## Parameters

### Search Mode

#### Use Regular Expressions
Enable [Java regular expression](http://download.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html) mode for all search patterns. When enabled, use `$1`, `$2`, etc. in replacement strings to reference captured groups.

In **both** modes, these escape sequences are supported:
- `
` = line-feed, `` = carriage-return, `	` = tab
- `\` = literal backslash
- `\uHHHH` = Unicode character (hex)

In **regex** mode, replacements additionally support:
- `$N` = match of search group N
- `\$` = literal dollar sign

> **Note:** You must use `\` to represent a literal backslash. For `\C` where `C` is not a special character, the result is the literal character (e.g., `\*` → `*`).

#### Dot Also Matches Line-Feed
Changes the meaning of `.` so it matches every character including `
`. Only available when regular expressions are enabled.

#### Ignore Case Differences
Makes all pattern matching case-insensitive. For example, `bear` matches `Bearcat`, `BEARCAT`, and `bearcat`. Only available when regular expressions are enabled.

#### Multiline Mode
Changes `^` and `$` to match at the beginning and end of any line, not just the whole string. Only available when regular expressions are enabled.

### Scope

#### Replace in Target Content
Performs search and replace on the **target** content of text units. Ignored in raw document mode.

> **Note:** When working on target content, you may need to use the **Create Target Step** before this step to ensure target content exists.

#### Replace in Source Content
Performs search and replace on the **source** content of text units. Ignored in raw document mode.

#### Replace All Instances
Replaces all matches found. If disabled, only the first match is replaced. In text unit mode, this applies per text unit; in raw document mode, it applies to the whole file.

### External Replacements

#### Replacements File Path
Path to a tab-delimited, UTF-8-encoded text file with two columns: search strings and replacement strings. Processed **after** the table expressions. Supports `${rootDir}` and `${inputRootDir}` variables.

> **Note:** Replacements from this file are limited to literal (non-regex) substring matching only.

### Logging

#### Save Replacement Log
Enables logging of all replacements performed.

#### Replacement Log File Path
Path where the replacement log will be saved. Supports `${rootDir}` and `${inputRootDir}` variables. Default: `${rootDir}/replacementsLog.txt`.

#### Replacement Count
Total number of replacements performed (output counter).

## Limitations

- When working with a raw document as input, the **entire content** is loaded into memory. This may cause problems with very large documents.
- The external replacements file is limited to literal (non-regex) substring searches only.
- Patterns are processed sequentially — longer patterns must be placed **first** if overlapping matches are possible.

## Notes

- Patterns are processed in declared order. Ensure longer patterns precede shorter overlapping ones.
- The external replacements file is processed **after** all inline table expressions.
- The `source` and `target` scope options only apply in filter events mode; they are ignored in raw document mode.

## Examples

### Escape Sequences

```
# Literal mode (regEx: false)
Search:  
  	 \ \uHHHH
Replace: 
  	 \ \uHHHH

# Regex mode adds (regEx: true)
Replace: $1 $2 ... $N (group refs), \$ (literal dollar)
```

### External Replacements File

Tab-delimited UTF-8 file with two columns:
```
old term	new term
foo	bar
```
