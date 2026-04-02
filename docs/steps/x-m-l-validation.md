# XML Validation Step

This step validates XML input documents for well-formedness and optionally performs structural validation against DTD, W3C XML Schema, or RelaxNG schema. Well-formedness checking is always performed regardless of validation settings — any non-XML format will fail this check. The step passes through raw XML documents unchanged.

## Parameters

#### Path of the XML Schema

Path to the XML Schema or RelaxNG schema file used for validation. Can also be a **URL** for W3C and RelaxNG schemas.

If left empty, the step will attempt to locate a schema declaration inside the document itself. If no schema is found in the document, validation will fail.

Only used when **Type of validation** is set to W3C Schema or RelaxNG.

> **Note:** Any schema declared in the documents themselves will also be used in addition to the one specified here.

> **Note:** For DTD validation, the DTD must be specified in the input documents — this path field is not used.

#### Validate Document Structure

Enables structural validation of documents in addition to the well-formedness check that always runs. When disabled, only XML well-formedness is verified. When enabled, the step performs a full DTD, W3C Schema, or RelaxNG validation depending on the selected **Type of validation**.

> **Note:** Well-formedness is **always** checked regardless of this setting. Non-XML content will fail even with validation disabled.

#### Type of Validation

Selects which validation method to use:

- **DTD** (0) — Validates against DTD declarations. The DTD must be specified in the input documents.
- **W3C Schema** (1) — Validates against a W3C XML Schema (`.xsd`). You can specify a schema path/URL or rely on schema declarations in the document.
- **RelaxNG Schema** (2) — Validates against a RelaxNG schema. You can specify a schema path/URL or rely on schema declarations in the document.

Only meaningful when **Validate Document Structure** is enabled.

#### Use DTD From Input Documents

Controls whether DTD declarations found inside the input documents are used for validation. When disabled, any `<!DOCTYPE>` declarations in the documents are ignored.

> **Note:** When performing DTD validation, the DTD **must** be specified in the input documents — there is no option to provide an external DTD path.

## Limitations

None known.

## Notes

- Well-formedness checking is **always** performed, even when validation is not selected.
- The step passes through raw XML documents unchanged.
- For DTD validation, the DTD must be declared within the input documents themselves.
- For W3C and RelaxNG schema validation, the schema path can be a URL.

## Examples

### Well-formedness check only

```yaml
validate: false
```

### DTD validation

Validate documents against DTD declarations embedded in the input files:

```yaml
validate: true
validationType: 0
useFoundDTD: true
```

### W3C Schema validation with external schema

```yaml
validate: true
validationType: 1
schemaPath: /path/to/schema.xsd
```

### RelaxNG validation via URL

```yaml
validate: true
validationType: 2
schemaPath: http://example.com/schema.rng
```
