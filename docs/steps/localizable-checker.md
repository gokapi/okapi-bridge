# Localizable Quality Check

This step checks that all localizables (dates, times, and numbers) exist in the target and are localized correctly for the target locale. It compares source and target number, date, and time patterns, adding issue annotations if a target localizable is missing or not properly localized. Both source and target languages must be set for the step to function.

## Parameters

This step has no configurable parameters.

## Limitations

- Numbers that use different units in the source and target will be flagged as false positives (e.g., imperial vs. metric measurements, currency conversions).

## Notes

- Requires both source and target language to be set in the pipeline.
- Produces issue annotations on segments — does not modify content.
- Checks dates, times (in various formats), and numbers.
