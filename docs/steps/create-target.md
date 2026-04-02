# Create Target Step

Creates the target segment container for each text unit in the pipeline. Optionally copies source content and/or source properties into the newly created target. This step operates on filter events and is typically placed early in a pipeline before any translation or leveraging steps.

## Parameters

#### Copy the source content to the target

Copies the source text into the newly created target container. This is useful when you want the target to start as a copy of the source — for example, before applying translation memory leveraging or machine translation.

If disabled, the target container is created empty.

**Default:** enabled

#### Copy the source properties to the target

Copies source-level properties (metadata attributes on the text unit) into the target. Properties may include state information, notes, or other metadata attached to the source entry.

If disabled, the target is created with no properties.

**Default:** enabled

#### Overwrite the current target content

Forces the source content and/or properties to be copied into the target **even when a target already exists**. Without this option, existing targets are left untouched.

Useful when re-processing bilingual files where you want to reset all targets back to source content.

> **Warning:** Enabling this will discard any existing translations in the target. Use with care on bilingual files that already contain translated content.

**Default:** disabled

#### Creates target for non-translatable text units

Controls whether a target container is created for text units marked as non-translatable. Some downstream steps may still need a target entry to exist even for non-translatable segments (e.g., for consistent output structure).

Disable this to skip target creation for non-translatable text units.

**Default:** enabled

## Limitations

- None known.

## Notes

- This step requires both source and target languages to be configured in the pipeline.
- Operates on filter events — must appear after a filter step in the pipeline.
- Typically placed early in the pipeline, before leveraging or translation steps.
