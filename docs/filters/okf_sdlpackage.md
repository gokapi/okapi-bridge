# SDL Trados Package Filter

The SDL Trados Package Filter processes SDL Trados SDLPPX (project package) and SDLRPX (return package) files. It extends the Archive Filter, detecting SDLXLIFF files for the specified language pair within the package and processing them using the XLIFF Filter with the `okf_xliff-sdl` configuration. Each SDLXLIFF file inside the package corresponds to a sub-document in the Okapi filter events.

## Processing Details

This filter is an extension of the Archive Filter. It reads the input package, detects the SDLXLIFF files for the specified language pair, and uses the XLIFF Filter (with `okf_xliff-sdl`) to process the content. Each SDLXLIFF file inside the package corresponds to a sub-document in the Okapi filter events.

## Parameters

This filter has no configurable parameters. It uses the Archive Filter infrastructure with built-in SDLXLIFF detection and the `okf_xliff-sdl` XLIFF Filter configuration.

## Limitations

- Reading an SDLPPX file and writing it back produces another SDLPPX file, not an SDLRPX file.
- All limitations of the XLIFF Filter also apply, since SDLXLIFF content is processed via the `okf_xliff-sdl` configuration.

## Notes

- The filter implementation class is `net.sf.okapi.filters.sdlpackage.SdlPackageFilter`.
- For XLIFF-level configuration details, see the XLIFF Filter documentation.
