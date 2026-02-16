# Okapi Bridge

A Java bridge for [gokapi](https://github.com/gokapi/gokapi) that provides access to [Okapi Framework](https://okapiframework.org/) filters. This enables gokapi to process 40+ document formats through Okapi's proven filter implementations.

## Features

- **40+ Filters**: HTML, XML, XLIFF, OpenXML, Markdown, JSON, YAML, PO, and many more
- **JSON Schemas**: Comprehensive parameter schemas for each filter with validation
- **Multi-version Support**: Builds for 10 Okapi versions (0.38 to 1.47.0)
- **NDJSON Protocol**: Go plugin interface via stdin/stdout

## Installation

Download a release from GitHub or use gokapi's plugin installer:

```bash
kapi plugins install okapi-bridge
```

## Schema Management

Each Okapi version has its own schemas and optional overrides:

```
okapi-releases/
  1.47.0/
    schemas/           # Generated JSON schemas for this version
    overrides/         # Optional UI hints (e.g., okf_json.overrides.json)
  1.46.0/
    schemas/
    overrides/
  ...
```

### Makefile Targets

```bash
make help              # Show all targets

# Discovery
make list-upstream     # Query Maven Central for available Okapi versions
make list-local        # List local okapi-releases/ directories

# Schema Management
make add-release V=1.48.0   # Create structure for new Okapi version
make regenerate V=1.47.0    # Regenerate schemas for one version
make regenerate-all         # Regenerate all versions

# Build
make build V=1.47.0    # Build JAR for specific version
```

## Adding a New Okapi Version

1. Check available versions:
   ```bash
   make list-upstream
   ```

2. Add the new version (creates directory and generates schemas):
   ```bash
   make add-release V=1.48.0
   ```

3. Optionally copy overrides from a previous version:
   ```bash
   cp okapi-releases/1.47.0/overrides/*.json okapi-releases/1.48.0/overrides/
   ```

4. Commit:
   ```bash
   git add okapi-releases/1.48.0
   git commit -m "feat: Add Okapi 1.48.0 support"
   ```

The CI/CD workflows automatically detect versions from `okapi-releases/`.

## Development

### Prerequisites

- Java 11+
- Maven 3.6+
- jq (for Makefile targets)

### Building

```bash
# Build with latest Okapi version
mvn package -Dokapi.version=1.47.0

# Run tests
mvn test -Dokapi.version=1.47.0
```

## License

Apache 2.0 (same as Okapi Framework)
