# Used Characters Listing Step

Generates a list of all characters used in the translatable text of a given set of documents. The output is a tab-delimited text file listing each character's Unicode code point, its visual representation, and occurrence count. Useful for creating minimal bitmap fonts (e.g., Japanese game interfaces) by including only the characters actually present in the source content.

Takes: Filter events. Sends: Filter events.

## Parameters

#### Path of the result file

Full path where the tab-delimited character listing file will be written.

The output file contains three tab-separated columns per line:
1. Unicode code point (e.g., `U+2192`)
2. Character representation (e.g., `'→'`)
3. Number of occurrences

Default: `charlist.txt`

#### Open the result file after completion

Automatically opens the result file in the system's default text editor after processing completes. Convenient for quick inspection of the character list during interactive use; disable for batch/automated pipelines.

Default: `true`

## Output Format

The result file is a tab-delimited text file. Example:

```
U+2192 '→' 50
U+00AE '®' 1
U+00A0 ' ' 3
U+007A 'z' 10
U+0079 'y' 320
```

The output list is not in any specific order.

## Limitations

None known.

## Notes

- Text units marked as non-translatable are ignored — only translatable content is scanned.
- The step passes filter events through unchanged, so it can be inserted into a pipeline without affecting downstream processing.
- This step is particularly useful when creating bitmap fonts for game interfaces, where including only used characters significantly reduces font file size.
