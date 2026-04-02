# XSL Transformation Step

Applies an XSLT template to input XML documents, producing a transformed raw document for the next pipeline step. Input documents must be well-formed XML. The step supports parameterized templates with built-in variables for input/output paths and locale information.

## Parameters

#### XSLT Template Path
Full path to the XSLT stylesheet (`.xsl` or `.xslt`) to apply to each input document. The template is applied to the raw XML input and the transformation result replaces the document for subsequent pipeline steps.

#### Template Parameters
Parameters passed to the XSLT template, one per line in `name=value` format.

Available variables for parameter values:

| Variable | Description |
|---|---|
| `${inputPath}` | Full path of the first input document |
| `${inputURI}` | URI of the first input document |
| `${outputPath}` | Full path of the first output document |
| `${srcLang}` | Source language code (e.g. `de` from `de-ch`) |
| `${trgLang}` | Target language code (e.g. `ja` from `ja-jp`) |
| `${inputPath2}`, `${inputPath3}` | Paths of 2nd/3rd input documents |
| `${inputURI2}`, `${inputURI3}` | URIs of 2nd/3rd input documents |

`${inputPath1}`, `${inputURI1}`, and `${outputPath1}` are aliases for the non-numbered variants.

Example:
```
Lang=en
tmFile=myTM.tmx
inputFile=${inputPath2}
```

#### Use Custom Transformer
Overrides the default JVM XSLT transformer and optionally the XPath engine with custom factory classes. The default transformer depends on your JVM implementation (see `TransformerFactory.newInstance()` for the lookup mechanism). Enable this to use a specific XSLT processor such as **Saxon**, which provides XSLT 2.0/3.0 support.

#### Transformer Factory Class
Fully qualified Java class name of the `TransformerFactory` implementation to use. The class must be on the Java classpath. Only applies when **Use Custom Transformer** is enabled.

Example: `net.sf.saxon.TransformerFactoryImpl`

#### XPath Factory Class
Fully qualified Java class name of the `XPathFactory` implementation to use. Allows overriding the default JVM XPath engine alongside the custom transformer. Only applies when **Use Custom Transformer** is enabled.

Example: `net.sf.saxon.xpath.XPathFactoryImpl`

#### Pass Output to Next Step
Controls whether the transformation output is passed forward as the raw document for subsequent pipeline steps. Enabled by default.

## Limitations

- None known.

## Notes

- Input documents must be well-formed XML — malformed XML will cause the transformation to fail.
- The default XSLT processor is determined by the JVM's `TransformerFactory.newInstance()` lookup, which typically provides XSLT 1.0 only. Use a custom transformer (e.g. Saxon) for XSLT 2.0/3.0 support.
- The step operates on raw documents — it does not use Okapi's filter event model.

## Examples

### Basic XSLT transformation
Apply a simple XSLT template with language parameters:
```yaml
xsltPath: /path/to/transform.xslt
paramList: |
  Lang=en
  tmFile=myTM.tmx
```

### Using Saxon for XSLT 2.0
Configure Saxon as the XSLT processor for XSLT 2.0/3.0 features:
```yaml
xsltPath: /path/to/transform-v2.xslt
useCustomTransformer: true
factoryClass: net.sf.saxon.TransformerFactoryImpl
xpathClass: net.sf.saxon.xpath.XPathFactoryImpl
```

### Using pipeline variables in parameters
Pass document paths and locale information to the stylesheet:
```yaml
xsltPath: /path/to/merge.xslt
paramList: |
  inputFile=${inputPath2}
  sourceLang=${srcLang}
  targetLang=${trgLang}
```
