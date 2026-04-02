# TM Import Step

Imports text units from an input document into a Pensieve TM. Each text unit (or its segments, if segmented) is added to the specified TM. If a text unit lacks text for the target locale, it is skipped. When segment counts differ between source and target, the whole text unit is imported instead of individual segments.

## Parameters

#### Directory of the TM where to import

Path to the directory containing the Pensieve TM. If the TM does not exist, it will be created automatically. If it already exists, new entries are added to the existing ones.

Supports the `${rootDir}` variable in the path.

#### Overwrite if source is the same

Controls how duplicate source texts are handled during import. When enabled, if an entry being imported has the same source text as one or more existing TM entries, **all matching entries are replaced** by the new one.

Leave disabled to allow multiple translations for the same source text (i.e., to preserve translation variants).

> **Warning:** Enabling this removes *all* existing entries with matching source text, not just one. If you have intentional translation variants for the same source, keep this disabled.

## Processing Notes

- Text units without text for the specified target locale are silently skipped.
- For segmented text units where source and target segment counts differ, the entire text unit is imported as a whole instead of segment-by-segment.
- Pass-through step: filter events are forwarded unchanged after import.

## Limitations

None known.

## Examples

### Basic TM import

```yaml
tmDirectory: "${rootDir}/project-tm"
overwriteSameSource: false
```

Import text units into a project-relative TM directory, creating the TM if it doesn't exist.

### Import with overwrite for updated translations

```yaml
tmDirectory: "${rootDir}/project-tm"
overwriteSameSource: true
```

When re-importing updated translations, overwrite existing entries that share the same source text to keep only the latest translation.
