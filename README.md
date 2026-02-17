# Okapi Bridge

A Java bridge for [gokapi](https://github.com/gokapi/gokapi) that provides access to [Okapi Framework](https://okapiframework.org/) filters. This enables gokapi to process 40+ document formats through Okapi's proven filter implementations.

## Features

- **57+ Filters**: HTML, XML, XLIFF, OpenXML, Markdown, JSON, YAML, PO, and many more
- **Centralized Schemas**: Deduplicated base schemas with composite versioning
- **Multi-version Support**: Builds for 10 Okapi versions (0.38 to 1.47.0)
- **NDJSON Protocol**: Go plugin interface via stdin/stdout
- **Per-version Dependencies**: Each Okapi version has its own discovered filter dependencies

## Installation

Download a release from GitHub or use gokapi's plugin installer:

```bash
kapi plugins install okapi-bridge
```

## Project Structure

```
okapi-bridge/
├── src/main/java/              # Bridge runtime and schema generator
├── schemas/                    # Centralized schema storage
│   ├── base/                   # Auto-generated base schemas (deduplicated)
│   └── composite/              # Merged schemas (base + overrides)
├── overrides/                  # Human-curated UI hints (single per filter)
│   ├── okf_html.overrides.json
│   └── ...
├── okapi-releases/             # Per-version configuration
│   ├── 1.47.0/
│   │   └── pom.xml            # Version-specific dependencies (auto-generated)
│   └── ...
├── scripts/
│   └── generate-version-pom.sh # Discovers filters for Okapi version
├── schema-versions.json        # Version tracking with composite hashes
├── pom.xml                     # Root pom (runtime + tests)
└── Makefile                    # Build automation
```

### Schema Architecture

The project uses a centralized schema architecture to reduce duplication:

1. **Base Schemas** (`schemas/base/`): Auto-generated from Okapi filter introspection. Named with content hash for deduplication (e.g., `okf_html.base.abc123.json`).

2. **Overrides** (`overrides/`): Human-curated UI hints (field grouping, widgets, presets). Single file per filter that applies to all versions.

3. **Composite Schemas** (`schemas/composite/`): Final schemas served to users (base + override merged). Versioned when composite content changes.

4. **schema-versions.json**: Index tracking base hash, override hash, and composite hash for each filter version.

### Composite Versioning

The composite version increments when either the base schema OR the override changes:

| Scenario | Base | Override | Composite | Result |
|----------|------|----------|-----------|--------|
| Initial | v1 | - | v1 | Same content |
| Add override | v1 | v1 | v2 | New version (override added) |
| Update override | v1 | v2 | v3 | New version (override changed) |
| Okapi release changes params | v2 | v2 | v4 | New version (base changed) |
| Okapi release, no changes | v1 | v2 | v3 | Same version (hash unchanged) |

## Schema Management

### Makefile Targets

```bash
make help              # Show all targets

# Discovery
make list-upstream     # Query Maven Central for available Okapi versions
make list-local        # List local okapi-releases/ directories

# Centralized Schema Management
make centralize           # Migrate to centralized schema structure
make regenerate-composites # Regenerate composites from base + overrides

# Add New Version
make add-release V=1.48.0  # Add new Okapi version (generates pom, schemas)

# Dependencies
make generate-pom V=1.47.0  # Generate version-specific pom.xml
make generate-all-poms      # Generate pom.xml for all versions

# Build
make build V=1.47.0    # Build JAR for specific version
make test              # Run tests
```

## Adding a New Okapi Version

1. Check available versions:
   ```bash
   make list-upstream
   ```

2. Add the new version (auto-generates pom.xml and updates centralized schemas):
   ```bash
   make add-release V=1.48.0
   ```

3. Commit and push:
   ```bash
   git add okapi-releases/1.48.0 schemas/ schema-versions.json
   git commit -m "feat: Add Okapi 1.48.0 support"
   git push
   ```

4. Create a release (triggers build for all versions):
   ```bash
   git tag v1.6.0
   git push origin v1.6.0
   ```

## Updating Overrides

When you need to improve a filter's UI (add field groups, custom widgets, etc.):

1. Edit the override file:
   ```bash
   vim overrides/okf_json.overrides.json
   ```

2. Commit and push (CI auto-regenerates composites):
   ```bash
   git add overrides/okf_json.overrides.json
   git commit -m "feat: Improve JSON filter UI grouping"
   git push
   ```

The CI workflow automatically:
- Regenerates all composite schemas
- Updates schema-versions.json with new hashes
- Auto-commits: `[ci] Regenerate composite schemas`

## CI/CD

### Nightly Release Check

A scheduled workflow runs daily to check Maven Central for new Okapi releases. If a new version is found, it automatically:
1. Discovers available filters and generates `pom.xml`
2. Generates base schemas and updates centralized composites
3. Creates a PR for review

### On Push to Main (CI)

- Builds and tests with the latest Okapi version
- Regenerates composite schemas if overrides changed
- Auto-commits regenerated files

### On Tag Push (Release)

1. **Setup job** scans `okapi-releases/` to get version list
2. **Build matrix** compiles a JAR for each Okapi version in parallel
3. **Release job** creates GitHub release with all artifacts
4. **Registry job** updates the gokapi plugin registry

## Development

### Prerequisites

- Java 11+
- Maven 3.6+
- jq (for Makefile targets)
- curl (for filter discovery)

### Building

```bash
# Build with version-specific dependencies
mvn package -f okapi-releases/1.47.0/pom.xml

# Build root project (bridge runtime)
mvn package

# Run tests
make test
```

### Schema Commands

```bash
# Regenerate all schemas from scratch
make centralize

# Regenerate composites only (after override changes)
make regenerate-composites
```

## License

Apache 2.0 (same as Okapi Framework)
