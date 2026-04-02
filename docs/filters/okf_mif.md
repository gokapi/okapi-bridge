# MIF Filter

The MIF Filter processes Adobe FrameMaker Interchange Format (MIF) documents. It supports MIF v8.0 and above, with automatic encoding detection based on file version. The filter extracts translatable text from body, hidden, master, and reference pages, and supports extraction of variables, index markers, and hypertext links.

The MIF 9.0 specification is available from Adobe.

## Parameters

### Options

#### Extract Variables
Extracts the **definitions** of FrameMaker variables so they can be translated. This covers all variable definitions in the document's variable catalog.

#### Extract Index Markers
Extracts index markers from extractable pages. Each index entry's text is extracted as a **separate text unit** placed *before* the text unit containing the index marker.

This only applies to pages that are enabled for extraction (body, hidden, master, or reference pages).

#### Extract Links
Extracts URLs from hypertext links in extractable pages. Each URL is extracted as a **separate text unit** placed *before* the text unit containing the hypertext marker.

This only applies to pages that are enabled for extraction.

#### Type of Page to Extract

##### Body Pages
Extracts translatable text from the **body pages** — the main content area of the FrameMaker document.

##### Hidden Pages
Extracts translatable text from **hidden pages** in the document.

##### Master Pages
Extracts translatable text from **master pages**, which define page layouts, headers, and footers.

##### Reference Pages
Extracts translatable text from **reference pages**.

> **Note:** By default, FrameMaker creates new documents with several reference pages that contain text — be aware of extra content when enabling this option.

### Inline Codes

#### Use Inline Code Rules
Enables inline code detection using regular expressions. Any match in extracted text is converted to an inline code.

The default expression matches FrameMaker building-block tags:
```
<\$.*?>
```
This matches patterns like `<$daynum>`, `<$monthname>`, `<$year>` commonly found in FrameMaker variable definitions.

## Limitations

- MIF versions below 8.0 are **not supported**.
- Very large embedded insets (e.g. images) may cause **Java heap memory issues**. The workaround is to link to external objects rather than embed them.
- The filter does not perform **font mapping** — if the translated file uses a language not supported by the source fonts, you must manually update the paragraph and character catalogs to use appropriate fonts.

## Notes

- Input encoding is **automatically detected** based on the MIF file version and document metadata.
- MIF v8 and above use **UTF-8** for both input and output.
- Index entries and link URLs are extracted as separate text units placed **before** the text unit containing their respective markers.

## Examples

### Default inline code pattern
The default code finder expression matches FrameMaker building-block variable tags like `<$daynum>`, `<$monthname>`, `<$year>`. These are converted to inline codes so translators see them as placeholders rather than translatable text.

```
<\$.*?>
```
