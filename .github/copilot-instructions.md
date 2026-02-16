# Copilot Instructions for Okapi Bridge

## Build & Test

```bash
# Build (requires specifying Okapi version)
mvn package -Dokapi.version=1.47.0

# Run all tests
mvn test -Dokapi.version=1.47.0

# Run a single test class
mvn test -Dokapi.version=1.47.0 -Dtest=ParameterApplierTest

# Run a single test method
mvn test -Dokapi.version=1.47.0 -Dtest=ParameterApplierTest#applyParameters_booleanValue_shouldApply
```

## Schema Management

```bash
# Add support for a new Okapi version (generates schemas and assigns versions)
make add-release V=1.48.0

# Regenerate schemas for one version (then run version-schemas)
make regenerate V=1.47.0

# Recompute schema versions across all releases
make version-schemas
```

Supported versions are derived automatically from `okapi-releases/` directories.

Schema versions (v1, v2, etc.) are tracked in `schema-versions.json` and increment when schema content changes between Okapi versions.

## Architecture

This is a Java bridge that exposes [Okapi Framework](https://okapiframework.org/) filters to Go applications via NDJSON over stdin/stdout.

### Protocol Flow

1. **OkapiBridgeServer** - Entry point, reads NDJSON commands from stdin, writes responses to stdout
2. **CommandHandler** - Dispatches commands (`open`, `read`, `write`, `close`, `list_filters`, `info`)
3. **FilterRegistry** - Maps filter class names to metadata, instantiates IFilter implementations
4. **EventConverter** - Converts Okapi Events to PartDTOs (JSON-serializable format)
5. **PartDTOConverter** - Applies translations back to Okapi Events for writing

### Schema Generation Pipeline

1. **ParameterIntrospector** - Reflects on Okapi IParameters classes to extract field metadata
2. **SchemaTransformer** - Converts introspected metadata to JSON Schema (draft-07)
3. **SchemaGenerator** - Produces `okf_*.schema.json` files for each filter
4. **SchemaVersioner** - Assigns version numbers based on content hash changes

### Directory Structure

```
okapi-releases/{version}/
  schemas/      # Generated JSON schemas (okf_*.schema.json)
  overrides/    # Optional UI hints (okf_*.overrides.json)
schema-versions.json  # Version history across all Okapi releases
```

## Conventions

- All logging goes to stderr; stdout is reserved for the NDJSON protocol
- Filter IDs use `okf_` prefix (e.g., `okf_html`, `okf_json`)
- The Okapi version is always passed via `-Dokapi.version=X.Y.Z` to Maven
- Some filters only exist in newer Okapi versions (see Maven profiles in pom.xml)
- Content is passed as base64 in the protocol to handle binary formats
- Overrides files enhance generated schemas with UI hints (groups, widgets, descriptions)
- Schema `$version` is `N.0.0` where N increments when the schema content changes
