# Rainbow Translation Kit Merging Step

This step merges the files of a Rainbow translation kit back into their original format. It processes the manifest file (`manifest.rkm`) created by the Rainbow Translation Kit Creation Step, combining translated XLIFF documents from the `work` folder with originals from the `original` folder. The step can output either filter events or raw documents depending on configuration.

## Parameters

#### Preserve the whitespace
Controls whether whitespace in the original content is preserved during the merging process.

#### Preserve the segmentation for the next steps
When merging a text unit, the content is un-segmented by default. Since this step is typically the **last step** in a pipeline, restoring segmentation would be unnecessary processing.

Enable this only if you have subsequent steps that need to work with segmented text units. Disabling this (the default) saves processing time.

#### Return raw documents instead of filter events
Controls what type of events are sent to subsequent steps in the pipeline:

- **Disabled** (default): Sends the current filter events being processed
- **Enabled**: Sends a raw document event for each merged output file

Use this when downstream steps expect raw document input rather than filter events.

#### Specify the target locale from the tool instead of the manifest
Forces the target locale to be taken from the tool/pipeline calling this step, rather than reading it from the manifest file. Useful when you need to override the locale specified in `manifest.rkm`.

#### Override the output path
Specify a directory where merged output files should be generated. When left empty, the output path defined in the manifest is used.

#### Copy the code metadata from the original file
Copies code metadata (inline code definitions, etc.) from the original source file into the merged output. Enabled by default.

## Limitations

- None known.

## Notes

- The input file must be the `manifest.rkm` file of the translation kit package.
- Translated XLIFF documents must be in the `work` folder of the package.
- Original documents must be in the `original` folder of the package.
- Options in each manifest file can also be edited when starting processing — see the Rainbow Translation Kit Manifest page for details.

## Examples

### Basic merging pipeline

The simplest pipeline to merge files of a translation kit:

```
= Raw Document to Filter Events Step
+ Rainbow Translation Kit Merging Step
```

The input file is the `manifest.rkm` file. This is one of the pre-defined pipelines available in Rainbow.
