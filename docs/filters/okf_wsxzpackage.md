# WSXZ Package Filter

The WSXZ Package Filter reads WSXZ (Worldserver TKIT) package files. It extends the Archive Filter, detecting SDLXLIFF files for the specified language pair and processing them with the XLIFF Filter (using the `okf_xliff-sdl` configuration). Each SDLXLIFF file inside the package corresponds to a sub-document in the Okapi filter events.

## Parameters

This filter has no configurable parameters. It uses the `okf_xliff-sdl` configuration of the XLIFF Filter internally.

## Limitations

- TMX and TBX files inside the package are ignored.
- All limitations of the XLIFF Filter also apply to the SDLXLIFF content processed within the package.

## Notes

- This filter is an extension of the Archive Filter.
- Each SDLXLIFF file in the package produces a separate sub-document in the event stream.
- The filter automatically identifies the correct SDLXLIFF files based on the specified language pair.
