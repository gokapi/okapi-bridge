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
│   ├── base/                   # Auto-generated base schemas (versioned)
│   │   ├── okf_html.v1.schema.json
│   │   └── okf_html.v2.schema.json  # New version when Okapi changes params
│   ├── composite/              # Merged schemas (base + overrides)
│   │   ├── okf_html.v1.schema.json
│   │   └── okf_html.v2.schema.json  # New version when base OR override changes
│   └── overrides/              # Human-curated UI hints (single per filter)
│       ├── okf_html.overrides.json
│       └── ...
├── okapi-releases/             # Per-version configuration
│   ├── 1.47.0/
│   │   └── pom.xml            # Version-specific dependencies (auto-generated)
│   └── ...
├── scripts/
│   ├── centralize-schemas.sh   # Orchestrates schema centralization
│   ├── compute-hash.sh         # Canonical JSON hashing
│   ├── merge-schema.sh         # Merges base + override with jq
│   └── generate-version-pom.sh # Discovers filters for Okapi version
├── schema-versions.json        # Version tracking with hashes
├── pom.xml                     # Root pom (runtime + tests)
└── Makefile                    # Build automation
```

### Schema Architecture

The project uses a centralized schema architecture to reduce duplication:

1. **Base Schemas** (`schemas/base/`): Auto-generated from Okapi filter introspection. Versioned per-filter (e.g., `okf_html.v1.schema.json`, `okf_html.v2.schema.json`).

2. **Overrides** (`schemas/overrides/`): Human-curated UI hints (field grouping, widgets, presets). Single file per filter that applies to all versions.

3. **Composite Schemas** (`schemas/composite/`): Final schemas served to users (base + override merged). Versioned when composite content changes.

4. **schema-versions.json**: Index tracking base version, base hash, override hash, and composite hash for each filter version.

### Composite Versioning

The composite version increments when either the base schema OR the override changes:

| Scenario | Base | Override | Composite | Result |
|----------|------|----------|-----------|--------|
| Initial | v1 | - | v1 | Same content |
| Add override | v1 | v1 | v2 | New version (override added) |
| Update override | v1 | v2 | v3 | New version (override changed) |
| Okapi release changes params | v2 | v2 | v4 | New version (base changed) |
| Okapi release, no changes | v1 | v2 | v3 | Same version (hash unchanged) |

<!-- SCHEMA_MATRIX_START -->
### Schema Statistics

- **Total filters**: 57
- **Total schema versions**: 185
- **Filters with version changes**: 50

### Schema Version Matrix

Shows which composite schema version applies to each Okapi release.
Only filters with multiple versions are shown (`-` = filter not available).

<details>
<summary>Click to expand full matrix</summary>

| Filter | 0.38 | 1.39.0 | 1.40.0 | 1.41.0 | 1.42.0 | 1.43.0 | 1.44.0 | 1.45.0 | 1.46.0 | 1.47.0 |
|--------|------|------|------|------|------|------|------|------|------|------|
| `okf_archive` | - | - | - | - | - | - | - | - | - | - |
| `okf_baseplaintext` | - | - | - | - | - | - | - | - | - | - |
| `okf_basetable` | - | - | - | - | - | - | - | - | - | - |
| `okf_commaseparatedvalues` | - | - | - | - | - | - | - | - | - | - |
| `okf_doxygen` | - | - | - | - | - | - | - | - | - | - |
| `okf_dtd` | - | - | - | - | - | - | - | - | - | - |
| `okf_epub` | - | - | - | - | - | - | - | - | - | - |
| `okf_fixedwidthcolumns` | - | - | - | - | - | - | - | - | - | - |
| `okf_html` | - | - | - | - | - | - | - | - | - | - |
| `okf_html5` | - | - | - | - | - | - | - | - | - | - |
| `okf_icml` | - | - | - | - | - | - | - | - | - | - |
| `okf_idml` | - | - | - | - | - | - | - | - | - | - |
| `okf_json` | - | - | - | - | - | - | - | - | - | - |
| `okf_markdown` | - | - | - | - | - | - | - | - | - | - |
| `okf_messageformat` | - | - | - | - | - | - | - | - | - | - |
| `okf_mif` | - | - | - | - | - | - | - | - | - | - |
| `okf_multiparsers` | - | - | - | - | - | - | - | - | - | - |
| `okf_odf` | - | - | - | - | - | - | - | - | - | - |
| `okf_openoffice` | - | - | - | - | - | - | - | - | - | - |
| `okf_openxml` | - | - | - | - | - | - | - | - | - | - |
| `okf_paraplaintext` | - | - | - | - | - | - | - | - | - | - |
| `okf_pdf` | - | - | - | - | - | - | - | - | - | - |
| `okf_phpcontent` | - | - | - | - | - | - | - | - | - | - |
| `okf_plaintext` | - | - | - | - | - | - | - | - | - | - |
| `okf_po` | - | - | - | - | - | - | - | - | - | - |
| `okf_properties` | - | - | - | - | - | - | - | - | - | - |
| `okf_rainbowkit` | - | - | - | - | - | - | - | - | - | - |
| `okf_regex` | - | - | - | - | - | - | - | - | - | - |
| `okf_regexplaintext` | - | - | - | - | - | - | - | - | - | - |
| `okf_sdlpackage` | - | - | - | - | - | - | - | - | - | - |
| `okf_splicedlines` | - | - | - | - | - | - | - | - | - | - |
| `okf_table` | - | - | - | - | - | - | - | - | - | - |
| `okf_tabseparatedvalues` | - | - | - | - | - | - | - | - | - | - |
| `okf_tex` | - | - | - | - | - | - | - | - | - | - |
| `okf_tmx` | - | - | - | - | - | - | - | - | - | - |
| `okf_transifex` | - | - | - | - | - | - | - | - | - | - |
| `okf_ts` | - | - | - | - | - | - | - | - | - | - |
| `okf_ttml` | - | - | - | - | - | - | - | - | - | - |
| `okf_ttx` | - | - | - | - | - | - | - | - | - | - |
| `okf_txml` | - | - | - | - | - | - | - | - | - | - |
| `okf_vignette` | - | - | - | - | - | - | - | - | - | - |
| `okf_vtt` | - | - | - | - | - | - | - | - | - | - |
| `okf_wiki` | - | - | - | - | - | - | - | - | - | - |
| `okf_wsxzpackage` | - | - | - | - | - | - | - | - | - | - |
| `okf_xini` | - | - | - | - | - | - | - | - | - | - |
| `okf_xliff` | - | - | - | - | - | - | - | - | - | - |
| `okf_xliff2` | - | - | - | - | - | - | - | - | - | - |
| `okf_xml` | - | - | - | - | - | - | - | - | - | - |
| `okf_xmlstream` | - | - | - | - | - | - | - | - | - | - |
| `okf_yaml` | - | - | - | - | - | - | - | - | - | - |

</details>
<!-- SCHEMA_MATRIX_END -->

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
make schema-matrix         # Generate schema version matrix

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
