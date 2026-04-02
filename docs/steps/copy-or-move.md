# Copy Or Move Step

This step copies or moves files from one location to another within the pipeline. It operates on raw documents and passes them through unchanged. Files are copied or moved into the location specified in the output settings tabs. Copy protection options control how conflicts with existing files are handled.

## Parameters

#### Choose Copy Protection Method

Controls how file conflicts are handled when the destination already contains a file with the same name:

- **Overwrite existing files** — Overwrites any existing files without prompt.
- **Backup existing files** — Creates a backup of any existing file before copying/moving.
- **Skip copy/move** — Skips copying/moving when a file already exists in the output location.

#### Move Files

When enabled, files are moved instead of copied (the source file is removed after transfer).

## Limitations

None known.

## Notes

- The destination location is configured in the pipeline's **output settings tabs**, not as a parameter on this step.
- This step passes through raw documents unchanged — it only affects file locations on disk.

## Examples

Use the step to copy source files to a target output directory. Configure the output location in the pipeline's output settings tab, and choose the appropriate copy protection method for your workflow.
