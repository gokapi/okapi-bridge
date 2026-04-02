# Batch Translation Step

The Batch Translation step creates translation memory entries from text units extracted from a raw document, using an external command-line tool to provide translations. It extracts content into a temporary HTML document, invokes the external translator, then aligns source and translated segments into a Pensieve TM and/or TMX document. Large documents are handled by processing text units in configurable blocks.

**Takes:** Raw document. **Sends:** Raw document (or TMX if configured).

## How It Works

1. The step extracts content from the raw document using its associated filter.
2. Extracted segments are written to a temporary HTML file (one `<p>` per segment).
3. The external tool is invoked via the configured command line — it must read `${inputPath}` and write translated HTML to `${outputPath}`.
4. The step reads back the translated HTML and aligns source/target segments.
5. Aligned entries are stored in a Pensieve TM and/or TMX document.
6. The original raw document is passed to the next step (unless **Send TMX** is enabled).

## Parameters

### Translation

#### Command Line
The command that invokes your external translation tool. The step creates a temporary HTML file where each paragraph is a source segment. Your tool must read it and produce a translated HTML file in the same format.

Available variables: `${inputPath}`, `${inputURI}`, `${outputPath}`, `${outputURI}`, `${srcLang}`, `${trgLang}`, `${srcLangName}`, `${trgLangName}`, `${rootDir}`, `${inputRootDir}`, plus locale-related variables.

#### Block Size
Maximum number of text units grouped into each temporary HTML file. Allows processing very large documents with tools that have size limitations.

#### Origin Identifier
Optional string stored as a property on each translated entry. In TMX output, appears as `<prop type="Txt::Origin">value</prop>`.

#### Mark as Machine Translation
Marks generated TM entries as machine translation by setting the `creationId` attribute to `"MT!"`.

### Segmentation

#### Segment Text Units
Segments extracted text units into sentences using SRX rules before building the temporary HTML. When enabled, each HTML paragraph is a sentence; when disabled, each paragraph is an unsegmented text unit.

> **Note:** Only entries processed by the external tool appear in TMX output. Entries found in existing TMs are not copied.

#### SRX Rules Path
Full path to the SRX segmentation rules file. Supports `${rootDir}`, `${inputRootDir}`, and locale variables.

### Output — Pensieve TM

#### Import into Pensieve TM
Imports translated entries into a Pensieve TM. If the TM does not exist, it is created; otherwise entries are appended. Entries are indexed per-document, so downstream steps only see entries from previously processed documents.

#### Pensieve TM Directory
Directory of the Pensieve TM to create or populate.

### Output — TMX

#### Create TMX Document
Creates a TMX document with all translated entries. A single file is generated across all input documents and is not written until the last document completes.

#### TMX Output Path
Full path of the TMX file to generate. Overwrites existing files.

#### Send TMX to Next Step
Sends the generated TMX document as the raw document to the next pipeline step instead of the original input document.

### Existing TM Lookup

#### Check Existing TM
Looks up each candidate entry in an existing Pensieve TM before translation. Entries with existing translations are skipped — not sent to the external tool and not included in TMX output.

#### Existing TM Directory
Directory of the existing Pensieve TM to check for pre-existing translations.

## Limitations

None known.

## Notes

- The temporary HTML preserves paragraph structure — the external tool must maintain the same number and order of paragraphs.
- Large documents are split across multiple invocations based on block size.
- The raw input document passes through unmodified unless **Send TMX** is enabled.
- Pensieve TM indexing is per-document, so downstream steps can only access entries from earlier documents in the batch.

## Examples

### Apertium MT on Linux

```yaml
command: "apertium -f html ${srcLang}-${trgLang} ${inputPath} ${outputPath}"
blockSize: 1000
markAsMT: true
makeTMX: true
tmxPath: "${rootDir}/output.tmx"
```

### ProMT on Windows

```yaml
command: "\"C:\Program Files\PRMT9\FILETRANS\FileTranslator.exe\" ${inputPath} /as /ac /d:${srcLangName}-${trgLangName} /o:${outputPath}"
blockSize: 500
markAsMT: true
origin: "promt-9"
makeTM: true
tmDirectory: "${rootDir}/tm"
```
