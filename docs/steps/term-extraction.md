# Term Extraction Step

Generates an output file containing a list of possible term candidates found in the source content of processed text units. Terms can be discovered through three independent methods: Terminology annotations, Text Analysis annotations, or simple statistical analysis on groups of tokens. The step processes all input documents before creating the candidate term list, and passes filter events through unchanged to the next step.

## Parameters

### Output

#### Output path

The generated file is **tab-delimited UTF-8** with one candidate term per line: occurrence count in the first column, term text in the second column. Supports the `${rootDir}` variable in the path.

#### Open the result file after completion

Automatically opens the generated result file in the system default application after the extraction process completes.

#### Sort the results by the number of occurrences

Controls the sort order of the output term list. Results are sorted by descending occurrence count. If not enabled, results are listed **alphabetically**.

### Extraction Methods

#### Use Terminology annotations

Extracts terms from `TermsAnnotation` annotations attached to text unit sources. These annotations are created by filters or other steps — for example, by ITS Terminology data category rules when using the XML Filter.

#### Use Text Analysis annotations

Extracts terms from spans annotated with the Text Analysis annotation on text unit source content. Such annotations are created by steps like the Enrycher Step.

#### Use tokens-grouping statistics

Finds term candidates using simple statistical analysis on groups of tokens (word sequences). A "term" is defined as a sequence of words meeting the configured minimum/maximum word count and minimum occurrence thresholds.

> **Note:** No stemming or lemmatization is performed — purely frequency-based extraction.

### Statistical Method Settings

These parameters are only used when **Use tokens-grouping statistics** is enabled.

#### Minimum number of words per term

Sets the lower bound on word count for a token sequence to qualify as a term candidate.

#### Maximum number of words per term

Sets the upper bound on word count for a token sequence to qualify as a term candidate. Keep this to a reasonable maximum — **6 or 7, or less** is recommended.

#### Minimum number of occurrences per term

A word sequence must appear at least this many times across **all input files** to be retained as a term candidate.

#### Preserve case differences

Controls whether case differences are preserved during token analysis. When enabled, `Term` and `term` are treated as **different** words. When disabled, they are treated as the same word and grouped together.

#### Remove entries that seem to be sub-strings of longer entries

Performs an extra post-processing pass that removes candidate terms that appear to be sub-sequences of longer candidates. For example:

Input:
- `gnu free` = 6
- `gnu free document` = 6
- `gnu free document license` = 5

Output after removal:
- `gnu free document` = 1
- `gnu free document license` = 5

> **Warning:** May cause loss of valid entries when they only appear as sub-sequences of longer, less relevant entries.

### Word Lists

These parameters are only used when **Use tokens-grouping statistics** is enabled. Leave paths empty to use the built-in English defaults.

#### Path of the file with stop words

Path to a plain-text file with one stop word per line. Stop words **break** a word sequence even if the maximum word count has not been reached.

> **Note:** The default English lists are embedded in the compiled resources at `net/sf/okapi/steps/termextraction` inside `okapi-lib-VERSION.jar`.

#### Path of the file with not-start words

Path to a plain-text file with one word per line. Not-start words are **excluded from the beginning** of a term but may appear within or at the end.

#### Path of the file with not-end words

Path to a plain-text file with one word per line. Not-end words are **excluded from the end** of a term but may appear within or at the beginning.

## Limitations

- None known.

## Notes

- The step processes **all** input documents before creating the term list — results are not available until the entire batch completes.
- Non-translatable entries are skipped.
- Filter events pass through to the next step unchanged.
- The output file is always UTF-8 encoded, tab-delimited.
- The three extraction methods are independent and can be combined.

## Examples

### Basic statistical term extraction

```yaml
outputPath: ${rootDir}/terms.txt
useStatistics: true
useTerminologyAnnotations: false
useTextAnalysisAnnotations: false
minWordsPerTerm: 2
maxWordsPerTerm: 5
minOccurrences: 3
removeSubTerms: true
sortByOccurrence: true
```

### Output format

```
12	user interface
8	source document
5	file format
3	translation memory
```
