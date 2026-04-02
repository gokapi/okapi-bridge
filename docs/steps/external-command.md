# External Command Step

This step runs a given external command on a raw document. It takes a raw document as input, executes the specified command line (with variable substitution for paths and locale information), and sends the resulting raw document downstream. The command must reference `${inputPath}` and `${outputPath}` to read and write the document.

## Parameters

#### Command Line

The command line to execute. Must reference `${inputPath}` and `${outputPath}` so the step knows where to read and write the document.

Available variables (case-sensitive):

| Variable | Description | Example |
|---|---|---|
| `${inputPath}` | Full path of the input document | |
| `${outputPath}` | Full path of the output document | |
| `${srcLangName}` | English name of source language | `de-ch` → `German` |
| `${trgLangName}` | English name of target language | `ja-jp` → `Japanese` |
| `${srcLang}` | Language code of source locale | `de-ch` → `de` |
| `${trgLang}` | Language code of target locale | `ja-jp` → `ja` |
| `${srcBCP47}` | BCP-47 tag of source locale (from M37) | `de-CH` → `de-CH` |
| `${trgBCP47}` | BCP-47 tag of target locale (from M37) | `ja-JP` → `ja-JP` |
| `${rootDir}` | Root directory for the project/batch | In Rainbow: the parameters folder |

> **Note:** The command **must** use `${inputPath}` and `${outputPath}` to correctly read input and produce output. Variable names are case-sensitive.

Example:
```
cmd /C "sort ${inputPath} /O ${outputPath}"
```

#### Timeout

Number of seconds to wait for the command to complete before cancelling it. Use `-1` to let the command run indefinitely.

## Limitations

- None known.

## Notes

- The step operates on **raw documents** — it does not parse or segment content. The external command receives the file as-is and must produce a complete output file.
- The `${srcBCP47}` and `${trgBCP47}` variables were introduced in milestone M37.

## Examples

### Sort file contents (Windows)

Uses the Windows `sort` command to sort the lines of the input document and write the sorted result to the output path.

```yaml
command: 'cmd /C "sort ${inputPath} /O ${outputPath}"'
timeout: -1
```
