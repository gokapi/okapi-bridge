# Leveraging Step

The Leveraging step sends extracted text units (or segments) to a translation resource — either a TM engine or an MT system — and retrieves matching translations. Matches above a configurable threshold are attached as annotations to the target, and optionally the best match can be copied directly into the target content. Non-translatable and approved text units are automatically skipped. Multiple Leveraging steps can be chained to query several resources in sequence (e.g., TM first, then MT for unmatched entries).

## Parameters

### General

#### Leverage Text Units
Master switch for the step. Allows you to keep the step configured in your pipeline while toggling it on and off without removing it.

#### Translation Resource
Fully qualified Java class name of the translation resource connector to use. Can be a TM connector (e.g., Pensieve) or an MT connector. The list of available connectors depends on your installation. Internet-based resources may be slow and result in lengthy processing time. See the Okapi **Connectors** documentation for available options.

#### No-Query Threshold
If a text unit already has a translation candidate with a score equal to or above this value, no query is sent to the translation resource. This avoids unnecessary (and potentially costly) lookups when a good-enough match already exists. Set to **101** to always query the resource regardless of existing candidates.

#### Minimum Match Score
Minimum match score for a translation to be accepted and attached as an annotation to the text unit. Matches below this score are discarded entirely.

#### Downgrade Identical Exact Matches
Reduces the score of identical exact matches by 1%. Some connectors return multiple exact matches that differ only in their translation. Because downstream components may trigger automated processes on exact matches, this prevents such triggers when the best match is ambiguous.

#### Copy Source If No Text
Copies the source content into the target when the source contains no translatable text (only whitespace and/or inline codes). This copy overwrites any existing target content.

### Fill Target

#### Fill Target with Translation
Copies the best translation candidate directly into the target content of the text unit, provided its score meets the fill target threshold. The match is still added as an annotation regardless of this setting.

#### Fill Target Threshold
Minimum match score for copying the best candidate into the target content. If the best match scores below this value, the translation is **not** copied into the target — but it is still attached as an annotation/candidate.

#### Only If Target Is Empty
Restricts target filling to only text units where the target is currently empty. Prevents overwriting existing translations.

#### Also If Target Equals Source
Allows overwriting target content when it is identical (text and codes) to the source. Useful for catching untranslated entries that were initialized with source content. Only available when "Only If Target Is Empty" is enabled.

### Target Prefix

#### Add Prefix to Translation
Adds a configurable prefix to the front of leveraged translations copied into the target, based on the match score. Useful for flagging fuzzy matches so translators can identify them.

#### Target Prefix Text
The prefix string inserted at the front of leveraged translations when their match score is at or below the prefix threshold. Default: `FUZZY__`.

#### Prefix Score Threshold
The prefix is added to the leveraged translation only when the best match score is **equal to or below** this value. Matches above this threshold are copied without a prefix.

### MT Prefix

#### Add MT Prefix to Source
Adds an MT-specific prefix to the source text when the match originates from an MT engine. This ensures the entry is treated as a fuzzy match in tools that cannot identify MT origin via the `creationid` attribute.

### TMX Output

#### Generate TMX Document
Creates a single TMX document containing all entries for which a match was found across all input files.

#### TMX Output Path
Full path for the generated TMX document. Supports variables: `${rootDir}`, `${inputRootDir}`, and locale variables (`${srcLoc}`, `${trgLoc}`, etc.).

## Limitations

None known.

## Notes

- Non-translatable text units are automatically skipped.
- Text units with the `approved` property set to `yes` are automatically skipped.
- Multiple Leveraging steps can be chained in a pipeline to query several translation resources in sequence (e.g., TM first, then MT for unmatched entries).
- Matches are attached as annotations to the target container or target segments regardless of the fill-target settings.
- Internet-based translation resources (e.g., cloud MT) may result in lengthy processing time.
- A single TMX output document is generated across all input files when TMX generation is enabled.

## Examples

### TM leveraging with fuzzy prefix

Leverage from a Pensieve TM, fill target for 95%+ matches, and prefix fuzzy matches for translator review.

```yaml
leverage: true
resourceClassName: net.sf.okapi.connectors.pensieve.PensieveTMConnector
threshold: 70
fillTarget: true
fillTargetThreshold: 95
useTargetPrefix: true
targetPrefix: "FUZZY__"
targetPrefixThreshold: 99
```

### Chained TM + MT leveraging

Use two Leveraging steps in sequence: the first queries a TM and the second uses MT only for entries without an existing good match. Set `noQueryThreshold` on the second step to skip entries already matched by the TM.

```yaml
# Step 1 — TM
leverage: true
resourceClassName: net.sf.okapi.connectors.pensieve.PensieveTMConnector
threshold: 70
fillTarget: true
fillTargetThreshold: 95
noQueryThreshold: 101

# Step 2 — MT (only for unmatched)
leverage: true
resourceClassName: net.sf.okapi.connectors.microsoft.MicrosoftMTConnector
threshold: 0
fillTarget: true
fillTargetThreshold: 0
noQueryThreshold: 95
useMTPrefix: true
```
