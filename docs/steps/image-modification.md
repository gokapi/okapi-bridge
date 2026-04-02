# Image Modification Step

This step modifies the size or format of image files. It takes a raw document containing an image and sends back a modified raw document. Supports scaling width and height independently, converting to grayscale, and changing the output image format.

## Parameters

#### Percentage of the original width

Sets the width of the output image as a percentage of the original. Use `100` for the same width as the original, `50` for half-width, `200` for double-width. Value must be greater than 0.

#### Percentage of the original height

Sets the height of the output image as a percentage of the original. Use `100` for the same height as the original, `50` for half-height, `200` for double-height. Value must be greater than 0.

#### Convert to gray scale

Converts all colors in the image to a corresponding set of grays, producing a grayscale output image.

#### Output format

Sets the image format for the output file. Leave empty (or use `<same as the original>`) to keep the original format. If the original format is not supported for output, a warning is generated and the output falls back to **PNG** if possible.

> **Note:** Selecting a specific output format does **not** automatically change the file extension — you may need a separate renaming step.

## Limitations

- Supported input image formats depend on the Java VM, its version, and the platform — not all formats may be available in all environments.
- Changing the output format does **not** automatically rename the file extension.

## Notes

- Width and height scaling are independent — non-uniform scaling is possible (e.g., 50% width with 100% height).
- The step operates on raw documents, so it must be placed in a pipeline path that routes image files as raw documents.

## Examples

### Scale images to half size

Reduces both width and height to 50% of the original, keeping the original format.

```yaml
scaleWidth: 50
scaleHeight: 50
format: ""
makeGray: false
```

### Convert to grayscale PNG

Converts images to grayscale and outputs as PNG format.

```yaml
scaleWidth: 100
scaleHeight: 100
format: png
makeGray: true
```
