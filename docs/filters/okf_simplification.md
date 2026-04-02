# Simplification Filter

The Simplification Filter is a **wrapper filter** that delegates document parsing to another (internal) filter and then transforms the generated events using the Resource Simplifier Step and/or the Inline Codes Simplifier Step. It simplifies filter output by flattening resources and merging or relocating inline codes. The default internal filter is `okf_xmlstream` (XML Stream Filter), but any registered Okapi filter configuration can be used.

## Parameters

#### Filter Configuration ID (`filterConfigurationId`)

The configuration ID of the filter that actually parses the document. The Simplification Filter instantiates this filter internally and post-processes its events.

Defaults to `okf_xmlstream`. You can use any filter configuration ID registered with Okapi (e.g., `okf_html`, `okf_json`, `okf_xml`).

#### Simplify Resources (`simplifyResources`)

Activates the **Resource Simplifier Step** on events from the internal filter. This performs resource flattening — removing references from resources so they become standalone text units.

At least one of `simplifyResources` or `simplifyCodes` **must** be enabled, otherwise the filter generates an error.

See also: [Resource Simplifier Step](Resource_Simplifier_Step)

#### Simplify Codes (`simplifyCodes`)

Activates the **Inline Codes Simplifier Step** on events from the internal filter. This merges adjacent inline codes in the source part of a text unit and moves leading/trailing codes from the source content into the skeleton.

At least one of `simplifyResources` or `simplifyCodes` **must** be enabled, otherwise the filter generates an error.

See also: [Inline Codes Simplifier Step](Inline_Codes_Simplifier_Step)

## Limitations

- This filter is **BETA**.

## Notes

- The filter internally creates and chains two pipeline steps (Resource Simplifier and Inline Codes Simplifier). Either or both can be activated via parameters.
- At least one simplification option must be active — enabling neither causes a runtime error.
- The internal filter is instantiated automatically from the provided configuration ID; you do not need to configure it separately.

## Examples

### Simplify XML Stream output

Default configuration: wraps `okf_xmlstream` and applies both resource and code simplification.

```yaml
filterConfigurationId: okf_xmlstream
simplifyResources: true
simplifyCodes: true
```

### Resource-only simplification for HTML

Wraps the HTML filter and only flattens resources, leaving inline codes untouched.

```yaml
filterConfigurationId: okf_html
simplifyResources: true
simplifyCodes: false
```
