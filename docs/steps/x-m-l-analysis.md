# XML Analysis Step

Generates an analysis report on the elements present in a set of input XML documents. The report helps guess localization-related aspects of the input documents, such as which elements should have their content extracted, which elements are likely inline, and whether there are any CDATA sections. Expects raw documents and passes them through unchanged.

## Parameters

#### Path of the result file

Full path where the HTML analysis report will be written. The report summarizes all XML elements found across the input documents, helping identify localization-relevant structures such as extractable content elements, inline elements, and CDATA sections.

The path can be absolute or relative to the working directory, and should end in `.html`.

**Examples:**
- `analysis.html` — writes to the current working directory
- `/tmp/reports/xml-analysis.html` — absolute path

#### Open the result file after completion

Automatically opens the generated report file in the system's default browser or HTML viewer after the pipeline completes. Useful during interactive sessions; disable for batch or CI workflows.

## Limitations

None known.

## Notes

- This step passes raw documents through unchanged — it only reads documents to build the report, it does not modify them.
- The output report is HTML format, summarizing XML element usage across all documents processed in the batch.
