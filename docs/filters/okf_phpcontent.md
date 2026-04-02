# PHP Content Filter

The PHP Content Filter processes PHP source code files, extracting translatable strings from PHP string literals (single-quoted, double-quoted, heredoc, and nowdoc syntax). It is based on the PHP language reference syntax specification. **This filter is not for HTML files with embedded PHP** — it processes only the content within `<?php ... ?>` tags. Many `.php` files are actually HTML with PHP tags and should use the HTML filter instead.

## Parameters

### Extraction

#### Use Localization Directives

Enables recognition of localization directives — special comments in the PHP source that override which strings are extracted. The directive syntax is shared across all Okapi filters.

If disabled, all directives in the input are ignored and every extractable string is extracted.

#### Extract Outside the Scope of the Directives

Controls whether translatable strings **outside** the scope of any localization directive are extracted.

- **Enabled**: Strings not covered by a directive are extracted (use directives to *exclude* specific strings)
- **Disabled**: Only strings explicitly within a directive scope are extracted (use directives to *include* specific strings)

This lets you choose the most efficient markup strategy — mark up the minority of strings that differ from the default behavior.

> **Note:** Only takes effect when **Use localization directives** is enabled.

### Inline Codes

#### Enable Inline Code Detection

Activates pattern-based detection of inline codes within extracted text. Matches from the regex rules are converted to inline code placeholders, protecting them from translation.

The default patterns detect:
- Partial HTML/XML tags at string boundaries
- Escape sequences (`
`, `	`, ``, etc.)
- Email addresses
- Template variables in brackets (`[var]`, `{var}`)

#### Inline Code Rules

Regex patterns that identify inline codes in translatable text. Each match is replaced with a placeholder code in the extracted segment.

Default patterns for PHP content:
```
(\A[^<]*?>)|(<[\w!?/].*?(>|\Z))
|(\a|\b|\f|\n|\r|\t|\v)
|(\w[-._\w]*\w@\w[-._\w]*\w\.\w{2,3})
|([\[{][\w_$]+?[}\]])
```

#### Merge Adjacent Codes

Merge consecutive inline codes into a single placeholder to simplify translatable segments.

#### Move Boundary Codes to Skeleton

Move inline codes at segment boundaries out of the translatable text into the non-translatable skeleton.

#### Simplifier Rules

Rules for simplifying inline code representation.

## Limitations

- The `define` statement is not supported — `define('KEY', 'value')` constructs are not extracted.
- In array declarations, **both** the string key and string value are extracted, which may result in unwanted items being sent for translation.

## Notes

- **Encoding detection priority**: BOM → `charset` declaration in first 1000 chars → default encoding. If both BOM and `charset` are present but disagree, the BOM encoding wins and a warning is generated.
- If the `charset` declaration value is literally the string `charset`, the file is treated as a template with no encoding declared.
- Output `charset` declarations are automatically updated to reflect the selected output encoding.
- **UTF-8 BOM handling**: A BOM is included in UTF-8 output only if the input was also UTF-8 *and* had a BOM.
- Line-break style (CRLF, LF) is preserved from input to output.

## Examples

### PHP content with extractable strings

String literals in assignments, heredoc blocks, and array values are all extractable:

```php
<?php
$str = <<<EOD
Example of string
spanning multiple lines
using heredoc syntax.
EOD;

class foo
{
    function foo()
    {
        $this->foo = 'Foo';
        $this->bar = array('Bar1', 'Bar2', 'Bar3');
    }
}

$name = 'MyName';

echo <<<EOT
My name is "$name". I am printing some $foo->foo.
Now, I am printing some {$foo->bar[1]}.
This should print a capital 'A': A
EOT;
?>
```
