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

This project tracks filter parameter schemas across 10 Okapi versions:

```
okapi-releases/
  versions.json              # Master config: supported versions, build versions
  0.38/schemas/              # Baseline schemas (schema v1)
  1.39.0/schemas/
  ...
  1.47.0/schemas/
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

# Versioning & Build
make version-schemas   # Run versioner across all releases → schemas/
make build V=1.47.0    # Build JAR for specific version
make build-releases    # Build JARs for all release versions
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

3. Update `okapi-releases/versions.json`:
   - Add version to `supported` array
   - Optionally add to `build` array for binary releases
   - Update `latest` if this is the newest version

4. Regenerate versioned schemas:
   ```bash
   make version-schemas
   ```

5. Commit the changes:
   ```bash
   git add okapi-releases/1.48.0 okapi-releases/versions.json
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

### Schema Generation

Schemas are generated from Okapi filter parameter classes using reflection:

```bash
# Generate schemas for a specific version
mvn exec:java@generate-schemas -Dexec.args="output-dir" -Dokapi.version=1.47.0
```

## License

Apache 2.0 (same as Okapi Framework)
