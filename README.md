# Okapi Bridge

A Java bridge for [gokapi](https://github.com/gokapi/gokapi) that provides access to [Okapi Framework](https://okapiframework.org/) filters. This enables gokapi to process 40+ document formats through Okapi's proven filter implementations.

## Features

- **40+ Filters**: HTML, XML, XLIFF, OpenXML, Markdown, JSON, YAML, PO, and many more
- **JSON Schemas**: Comprehensive parameter schemas for each filter with validation
- **Version Tracking**: Schemas track changes across 10 Okapi versions (0.38 to 1.47.0)
- **gRPC Protocol**: Go plugin interface via HashiCorp go-plugin

## Installation

Download a release from GitHub or use gokapi's plugin installer:

```bash
kapi plugins install okapi-bridge
```

## Schema Management

Pre-generated schemas are stored in `okapi-releases/{version}/schemas/` directories:

```
okapi-releases/
  0.38/schemas/              # Baseline schemas (schema v1)
  1.39.0/schemas/
  ...
  1.47.0/schemas/

schemas/                     # Versioned output (committed to git)
  *.schema.json              # Latest schemas with version metadata
  schema-versions.json       # Version history across all Okapi releases
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
make version-schemas        # Compute versioned schemas → schemas/

# Build
make build V=1.47.0    # Build JAR for specific version
```

## Adding a New Okapi Version

1. Check available versions:
   ```bash
   make list-upstream
   ```

2. Add the new version:
   ```bash
   make add-release V=1.48.0
   ```

3. Update `Makefile`:
   - Add version to `SUPPORTED_VERSIONS`
   - Update `LATEST_VERSION` if this is the newest

4. Update `.github/workflows/release.yml`:
   - Add version to `matrix.okapi-version` if you want binaries

5. Regenerate versioned schemas and commit:
   ```bash
   make version-schemas
   git add okapi-releases/1.48.0 schemas/ Makefile
   git commit -m "feat: Add Okapi 1.48.0 schemas"
   ```

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
