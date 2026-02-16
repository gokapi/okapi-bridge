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
schema-versions.json   # Version history tracking changes across releases
```

Schemas include version metadata (`$version`, `x-schemaVersion`, `x-okapiVersions`) that tracks when each schema changed. For example, `okf_json` is at v3, introduced in Okapi 1.46.0.

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
make version-schemas        # Recompute schema versions across all releases

# Build
make build V=1.47.0    # Build JAR for specific version
```

## Adding a New Okapi Version

1. Check available versions:
   ```bash
   make list-upstream
   ```

2. Add the new version (creates directory, generates schemas, assigns versions):
   ```bash
   make add-release V=1.48.0
   ```

3. Optionally copy overrides from a previous version:
   ```bash
   cp okapi-releases/1.47.0/overrides/*.json okapi-releases/1.48.0/overrides/
   ```

4. Commit and push:
   ```bash
   git add okapi-releases/1.48.0 schema-versions.json
   git commit -m "feat: Add Okapi 1.48.0 support"
   git push
   ```

5. Create a release (triggers build for all versions):
   ```bash
   git tag v1.6.0
   git push origin v1.6.0
   ```

## CI/CD

The workflows automatically detect Okapi versions from the `okapi-releases/` directory.

### On Push to Main (CI)

- Builds and tests with the latest Okapi version
- No manual configuration needed when adding versions

### On Tag Push (Release)

1. **Setup job** scans `okapi-releases/` to get version list and latest version
2. **Build matrix** compiles a JAR for each Okapi version in parallel
3. **Release job** creates GitHub release with all artifacts
4. **Registry job** updates the gokapi plugin registry

Each Okapi version produces a separate artifact:
- `okapi-bridge-v1.6.0-okapi1.47.0.tar.gz` (latest, installed as `okapi-bridge`)
- `okapi-bridge-v1.6.0-okapi1.46.0.tar.gz` (installed as `okapi-bridge-1.46.0`)
- etc.

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
