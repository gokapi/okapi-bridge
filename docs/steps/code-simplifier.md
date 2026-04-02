# Inline Codes Simplifier

This step joins adjacent inline codes in text units, and optionally moves leading and trailing codes from the text unit to the skeleton. Only the source codes are affected, so it should be used **before** any target is created in the text unit. Takes filter events and sends filter events.

## Parameters

#### Move leading and trailing codes to skeleton

Removes leading and trailing inline codes from text units and places them into the skeleton. This reduces the number of codes translators need to deal with, making segments cleaner.

Only works with `GenericSkeleton`-based filters.

> **Note:** Only affects source codes — apply this step **before** any target is created in the text unit.

#### Merge adjacent codes

Merges adjacent inline codes in text units into a single code. This simplifies segments where multiple codes appear next to each other with no translatable text between them.

> **Note:** Only affects source codes — apply this step **before** any target is created in the text unit.

## Limitations

None known.

## Notes

- Only source codes are affected — this step should be placed in the pipeline **before** any step that creates target content in text units.
- Leading/trailing code removal only works with `GenericSkeleton`-based content.
