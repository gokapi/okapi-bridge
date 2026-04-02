# Microsoft Batch Translation

This step annotates text units with [Microsoft Translator](http://www.microsofttranslator.com) candidates and/or creates a TMX translation memory from them. It processes multiple text units in batches for significantly faster throughput compared to using the Leveraging Step with the Microsoft Translator Connector individually. Text units flagged as non-translatable are skipped.

## Parameters

### Authentication

#### Azure Key
The Azure subscription key used to authenticate with the Microsoft Translator v3 API. If you previously had a v2 key, the same key should work with v3. Obtain a key from the Azure Cognitive Services pricing page.

> **Note:** The v2 API was retired on 2019-04-30. You must use the v3 API with an Azure key.

> **Note:** You must respect Microsoft's Terms of Service. Commercial or high-volume usage requires a commercial license agreement.

### Engine Selection

#### Category
An optional identifier for accessing a trained custom engine from Microsoft Translator Hub. You can specify either a direct engine identifier or a keyword in the form `@@@keyword@@@` (requires an engine mapping file). The `${domain}` variable can be used to dynamically select engines based on ITS Domain annotations.

> **Note:** Only the first occurrence of the ITS Domain annotation has an effect on engine selection.

> **Note:** Because this step works in batches, segments before the first Domain annotation but within the same batch will be translated with the domain engine.

#### Engine Mapping
Path to a properties file mapping category keywords to engine identifiers. Format: `<keyword>.<LANGUAGE>=<engineID>`. Supports `${rootDir}`, `${inputRootDir}`, and locale variables. A fallback mechanism tries progressively shorter keyword prefixes.

### Query Settings

#### Events Buffer
Controls how many filter events are buffered before sending a single batch query. Larger buffers improve throughput but may hit API text volume limits.

#### Maximum Matches
Limits the number of translation candidates returned per source text segment.

#### Threshold
Translation matches scoring below this value are discarded.

#### Query Only Entries Without Existing Candidate
Only sends text units to Microsoft Translator when they have no existing translation candidates from previous steps or the original document.

### Output Options

#### Annotate the Text Units with the Translations
Adds translation match annotations to text units for use by subsequent pipeline steps. Existing annotations are preserved.

#### Fill the Target with the Best Translation Candidate
Copies the highest-scoring translation candidate into empty target segments when the score meets or exceeds the fill threshold.

#### Fill Threshold
The minimum score required before a translation candidate is copied into the target.

#### Add a Prefix to the Translation Candidate
Prepends a configurable prefix string to translations copied into the target for visual flagging during review.

#### Target Prefix
The text string prepended to translation candidates (default: `!MT__`).

#### Mark as Machine Translation
Marks generated TM entries with a machine translation origin indicator (e.g., `creationId="MT!"`).

### TMX Output

#### Generate a TMX Document
Creates a TMX translation memory file from the translation results.

#### TMX Output Path
Full file path for the generated TMX document. Supports `${rootDir}`, `${inputRootDir}`, and locale variables.

#### Send the TMX Document to the Next Step
Passes the generated TMX document as input to the next pipeline step instead of filter events.

## Limitations

- The Microsoft Translator API has restrictions for high-volume usage — contact Microsoft for details.
- Only the first ITS Domain annotation encountered in a batch affects engine selection.
- The v3 API no longer supports the translation memory function that was available in v2.
- The v3 implementation does not support newer features such as profanity filtering.

## Notes

- Text units flagged as non-translatable are not sent for translation.
- Using the Leveraging Step with the Microsoft Translator Connector produces similar results, but this batch step is **much faster** because it processes multiple text units per API call.
- MT output quality can sometimes be improved post-processing — e.g., spaces around inline codes can be fixed with the Space Check Step.

## Examples

### Engine Mapping File
```properties
travel.FR=11111111-2222-3333-4444-e42f530c98b8_tra
client1.DE=11111111-2222-3333-4444-90dd26cc9dsd3_gen
client1.tech.DE=11111111-2222-3333-4444-90dd26cc9d48_tech
client2.DE=11111111-2222-3333-4444-90dd26cc9ds34_gen
```

To use the first engine (translating into French), set Category to `@@@travel@@@`. To use the third engine, set it to `@@@client1.tech@@@`. A fallback mechanism applies: `@@@client2.law@@@` first looks for `client2.law.DE`, then falls back to `client2.DE`.
