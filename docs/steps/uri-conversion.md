# URI Conversion Step

Converts translatable content of input documents to and from [URI](http://en.wikipedia.org/wiki/URI) escape notation. Can either un-escape URI escape sequences back to normal text, or escape normal text characters into URI percent-encoded sequences. The set of characters to escape is configurable, with presets based on [RFC 2396](http://tools.ietf.org/html/rfc2396) mark and reserved character classes.

Takes: Filter events. Sends: Filter events.

## Parameters

#### Conversion Direction

Controls the direction of URI conversion:

- **0** — Un-escape URI escape sequences (e.g. `%20` → space). Converts percent-encoded sequences back into normal text.
- **1** — Escape content to URI escape sequences (e.g. `&` → `%26`). Converts characters in the escape list into percent-encoded form.

When escaping, which characters get converted is controlled by the escape list and the extended characters option.

#### Escape All Extended Characters

Escapes all non-ASCII (extended) characters to URI escape sequences using UTF-8 percent-encoding. This applies in addition to the characters specified in the escape list. Only meaningful when conversion direction is set to escape mode.

#### Characters to Escape

The set of characters that will be converted to URI escape sequences when escaping. Each character in the string is escaped individually.

> **Note:** The `%` character is **always** escaped regardless of whether it appears in this list.

Common presets based on RFC 2396:

- **All But Marks** — escape everything except RFC 2396 "marks": `-`, `_`, `.`, `!`, `~`, `*`, `'`, `(`, `)`
- **All But Marks And Reserved** — escape everything except marks and RFC 2396 "reserved" characters: `;`, `/`, `?`, `:`, `@`, `&`, `=`, `+`, `$`, `,`

Only meaningful when conversion direction is set to escape mode.

## Limitations

- None known.

## Notes

- The `%` character is always escaped when in escape mode, regardless of the escape list configuration.
- Extended (non-ASCII) characters are encoded using UTF-8 percent-encoding when the extended characters option is enabled.
- The step operates on translatable content within filter events — non-translatable content passes through unchanged.

## Examples

### Decode URI-encoded content

Un-escape percent-encoded sequences in translatable text back to readable characters.

```yaml
conversionType: 0
```

Input: `Hello%20World%21` → Output: `Hello World!`

### Encode content for URI safety

Escape special characters in translatable text to URI percent-encoded form using the default escape list.

```yaml
conversionType: 1
escapeList: "%{}[]()&"
updateAll: false
```

Input: `key={value}&other=(test)` → Output: `key=%7Bvalue%7D%26other=%28test%29`
