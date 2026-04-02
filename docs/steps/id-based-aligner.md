# Id-Based Aligner

Aligns text units from two input files by matching their ids (taken from `TextUnit.getName()`). Works with any filter that produces unique names for its text units, such as the Properties Filter. Can either produce aligned bilingual text units in the pipeline or generate a TMX file as output.

Takes: filter events. Sends: filter events.

The **source** file is the first (primary) input and provides source content. The **target** file is the second input and provides target content. If the target file is monolingual (e.g., Java properties), the source extracted from that file is used as target content. If the target file is multilingual (e.g., XLIFF), the actual target is used, and an additional source-text match check is performed.

## Parameters

#### Generate a TMX file

Controls the step's output mode:

- **Checked**: produces a TMX file at the specified output path. The filter events passed through the pipeline are **unchanged**.
- **Unchecked**: each text unit in the returned events is replaced with a new bilingual text unit containing aligned source and target content.

> **Note**: for target `<tuv>` data to appear in the generated TMX, the **Copy to/over the target** option must also be checked.

#### TMX output path

Full path for the generated TMX file. Only used when **Generate a TMX file** is enabled.

#### Fall back to source text

When a source text unit has no matching target, the source text is used as the target content instead. Useful for creating complete bilingual files where missing translations fall back to the original language.

#### Copy to/over the target

Copies the aligned target content directly onto the text unit's target.

> **WARNING**: Copied target will not be segmented and any existing target content will be lost.

If the entry from the target file is marked as *approved*, the approved property is preserved. An alternate translation annotation is still created if that option is also enabled.

#### Create an alternate translation annotation

Attaches the matched target as an `AltTranslation` annotation on the text unit. This makes the aligned target visible to subsequent pipeline steps without overwriting the text unit's actual target.

#### Suppress TUs with no target

Filters out text units that have no matching target from the pipeline output. Without this option, unmatched source text units generate a warning but are still passed through.

#### Align based on TextUnit IDs

Switches the matching key from `TextUnit.getName()` (resource name) to `TextUnit.getId()` (text unit ID). Use this when the filter produces meaningful IDs rather than names for matching.

## Limitations

- Assumes each text unit has a **unique** name (or ID) value. Filters that produce duplicate names will cause incorrect alignments.
- Aligns at the **text unit level only** — segments within text units are not individually aligned.
- Both input files are expected to be **non-segmented**.
- For multilingual target files, alignment requires both matching ID **and** matching source text — if source texts differ, no alignment is made.

## Notes

- Text units present in the source but missing from the target generate a **warning**. Text units in the target but not in the source are silently ignored.
- If the target file is monolingual, the source extracted from that file is used as target content.
- If the target file is multilingual, in addition to matching on ID, the step verifies that both text units have the same source text before aligning.

## Examples

### Align two properties files into bilingual text units

Aligns a source English `.properties` file with a target French `.properties` file, producing bilingual text units in the pipeline.

```yaml
generateTMX: false
replaceWithSource: true
storeAsAltTranslation: true
copyToTarget: false
```

### Generate a TMX from two properties files

Produces a TMX file from aligned properties files. Both `generateTMX` and `copyToTarget` must be enabled for target `<tuv>` data to appear in the output.

```yaml
generateTMX: true
tmxOutputPath: /output/en-fr-aligned.tmx
copyToTarget: true
storeAsAltTranslation: true
```
