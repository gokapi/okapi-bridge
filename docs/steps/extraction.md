# Rainbow Translation Kit Creation Step

Creates a translation package (Rainbow translation kit) from a set of input documents processed through Okapi filters. Supports multiple package formats including Generic XLIFF, PO, OmegaT, Transifex, XLIFF 2.0, and others. The extracted files can be merged back after translation using the Rainbow Translation Kit Merging Step.

Takes: Filter events. Sends: Filter events or raw documents.

## Parameters

### Package Format

#### Package Format (`writerClass`)

Fully qualified Java class name of the package writer that determines the output format of the translation kit.

Available formats:
- **Generic XLIFF** — files extracted to XLIFF documents
- **PO Package** — files extracted to PO files
- **Original with RTF** — files get an RTF layer
- **XLIFF with RTF** — files extracted to XLIFF then get an RTF layer
- **OmegaT Project** — files extracted to a new OmegaT project
- **Transifex Project** — files uploaded to a Transifex project
- **ONTRAM XINI** — files extracted to ONTRAM XINI format
- **Translation Table** — files extracted to tab-delimited files
- **XLIFF 2.0** — files extracted to XLIFF v2 documents

Each writer may have its own configuration options. Not all formats support per-file output.

### Output Location

#### Output Root Directory (`packageDirectory`)

Root directory where the translation package will be created. Supports variable substitution:
- `${rootDir}` — pipeline root directory
- `${inputRootDir}` — root directory of the input files
- Locale variables: `${srcLoc}`, `${trgLoc}`, etc.

#### Package Name (`packageName`)

Name of the package directory created inside the output root directory.

#### Create ZIP Package (`createZip`)

Creates a ZIP archive of the generated package with a `.rkp` extension, placed in the same directory as the package. A package named `pack1` produces `pack1.rkp`.

### Support Material

#### Support Material Files (`supportFiles`)

Additional files to include in the translation package. Each item specifies an origin path and a destination within the package.

**Origin** paths support `*` and `?` wildcards, plus variables (`${rootDir}`, `${inputRootDir}`, locale variables). **Destination** paths must start with `/` or `\` (relative to package root). Use `<same>` to keep the original filename.

#### Package Message (`message`)

Optional message or instructions to include with the translation package for translators.

### Output Options

#### Output Manifest (`outputManifest`)

Controls whether a manifest file is generated. The manifest is required by the Rainbow Translation Kit Merging Step to merge translated files back.

> **Warning:** Disabling this will prevent the package from being merged back.

#### Send Output to Next Step (`sendOutput`)

Sends the prepared files to the next pipeline step as raw documents instead of the original filter events.

> **Note:** This option has no effect if the selected package format does not support one output file per input file.

## Limitations

- If this step follows a pipeline step that modifies the input file (e.g., Search and Replace), the "original" files saved in the package may not match the extracted content, making merge-back impossible without manual correction.
- Not all package formats support one output file per input file, which limits the `sendOutput` option.

## Notes

- The step expects filter events as input — typically preceded by a Raw Document to Filter Events Step.
- Extracted packages can be merged back using the Rainbow Translation Kit Merging Step, provided the manifest was generated.
- The step can be combined with segmentation and leveraging steps to pre-translate content before kit creation.

## Examples

### Simple extraction pipeline

Minimal pipeline that extracts documents into a Rainbow translation kit:

```yaml
pipeline:
  - step: raw-document-to-filter-events
  - step: extraction
    params:
      packageName: pack1
      packageDirectory: ${inputRootDir}
```

### Pre-translated kit with segmentation and leveraging

Segments text and attempts pre-translation before creating the kit:

```yaml
pipeline:
  - step: raw-document-to-filter-events
  - step: segmentation
  - step: leveraging
  - step: leveraging
  - step: extraction
    params:
      packageName: project-kit
      packageDirectory: ${inputRootDir}/output
      createZip: true
```
