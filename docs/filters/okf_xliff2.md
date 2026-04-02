# XLIFF-2 Filter

The XLIFF-2 Filter implements the `IFilter` interface for **XLIFF 2.x** (XML Localisation Interchange File Format) documents. XLIFF v2 is an OASIS Standard for transporting translatable text and localization-related information across translation and localization tool chains. The filter provides basic support for XLIFF 2.x core including extended attributes/namespaces, segments and ignorables, inline codes, notes, groups, and the XLIFF 2.x Metadata Module.

## Parameters

#### Maximum Validation
Ensures the XLIFF-2 parser performs the **maximum verification** of the format during parsing. Useful for catching malformed or non-compliant XLIFF 2.x files early in the pipeline.

#### Needs Segmentation
Resegments any unit that is marked with `canSegment="yes"` in the XLIFF source.

> **Note:** Units that already have a `<target>` element **cannot** be segmented, even if `canSegment="yes"` is set.

## Limitations

- Skeleton not supported.
- Comments are lost in the merged document.
- Original XML formatting is lost in the merged document.
- Attributes can be reordered.
- Attributes may be removed or added compared to the original depending on default values and logic in the XLIFF 2 Toolkit.

## Notes

- The filter supports XLIFF 2.x core features: extended attributes and namespaces, segments and ignorables, inline codes, notes, groups, and the XLIFF 2.x Metadata Module.
- Merged documents will not preserve original XML formatting, comments, or attribute ordering.
- The XLIFF 2.0 specification is available at the OASIS website.
