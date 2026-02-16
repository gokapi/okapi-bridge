# Okapi Bridge Makefile
# Manages schema generation across Okapi versions

SHELL := /bin/bash
.PHONY: help list-upstream list-local add-release regenerate regenerate-all \
        version-schemas build build-releases clean test

# Configuration
VERSIONS_FILE := okapi-releases/versions.json
SCHEMA_OUTPUT := schemas

# Default target
help:
	@echo "Okapi Bridge - Schema and Release Management"
	@echo ""
	@echo "Discovery:"
	@echo "  make list-upstream        Query Maven Central for available Okapi versions"
	@echo "  make list-local           List local okapi-releases/ directories"
	@echo ""
	@echo "Schema Management:"
	@echo "  make add-release V=1.48.0 Create structure for new Okapi version and generate schemas"
	@echo "  make regenerate V=1.47.0  Regenerate schemas for one version"
	@echo "  make regenerate-all       Regenerate schemas for all supported versions"
	@echo ""
	@echo "Versioning:"
	@echo "  make version-schemas      Run SchemaVersioner across all versions -> schemas/"
	@echo ""
	@echo "Build:"
	@echo "  make build V=1.47.0       Build JAR for specific Okapi version"
	@echo "  make build-releases       Build JARs for all release versions"
	@echo "  make test                 Run tests"
	@echo "  make clean                Clean build artifacts"

# ============================================================================
# Discovery
# ============================================================================

list-upstream:
	@echo "Available Okapi versions in Maven Central:"
	@curl -s "https://search.maven.org/solrsearch/select?q=g:net.sf.okapi+AND+a:okapi-core&core=gav&rows=50&wt=json" \
		| jq -r '.response.docs[].v' | sort -V

list-local:
	@echo "Local okapi-releases/ directories:"
	@ls -1 okapi-releases/ 2>/dev/null | grep -E '^[0-9]' || echo "  (none)"
	@echo ""
	@echo "Supported versions (from versions.json):"
	@jq -r '.supported[]' $(VERSIONS_FILE) 2>/dev/null || echo "  (versions.json not found)"
	@echo ""
	@echo "Build versions:"
	@jq -r '.build[]' $(VERSIONS_FILE) 2>/dev/null || echo "  (versions.json not found)"

# ============================================================================
# Schema Management
# ============================================================================

# Ensure schema generator is compiled
.compile-generator:
	@echo "Compiling schema generator..."
	@mvn -B -q compile -Dokapi.version=1.47.0
	@touch .compile-generator

# Add a new Okapi release
add-release: .compile-generator
ifndef V
	$(error V is required. Usage: make add-release V=1.48.0)
endif
	@echo "Adding Okapi release $(V)..."
	@mkdir -p okapi-releases/$(V)/schemas
	@mkdir -p okapi-releases/$(V)/overrides
	@# Generate schemas
	@echo "Generating schemas for Okapi $(V)..."
	@mvn -B -q dependency:resolve -Dokapi.version=$(V) 2>/dev/null || true
	@mvn -B -q compile -Dokapi.version=$(V)
	@mvn -B exec:java@generate-schemas -Dexec.args="okapi-releases/$(V)/schemas" -Dokapi.version=$(V)
	@# Create meta.json
	@echo '{"okapiVersion":"$(V)","generatedAt":"'$$(date -u +%Y-%m-%dT%H:%M:%SZ)'","filterCount":'$$(ls -1 okapi-releases/$(V)/schemas/*.schema.json 2>/dev/null | wc -l | tr -d ' ')'}' \
		| jq . > okapi-releases/$(V)/schemas/meta.json
	@echo "Created okapi-releases/$(V)/ with $$(ls -1 okapi-releases/$(V)/schemas/*.schema.json | wc -l | tr -d ' ') schemas"
	@echo ""
	@echo "Next steps:"
	@echo "  1. Review generated schemas in okapi-releases/$(V)/schemas/"
	@echo "  2. Add any overrides in okapi-releases/$(V)/overrides/"
	@echo "  3. Update versions.json to include $(V) in supported/build arrays"
	@echo "  4. Run 'make version-schemas' to update versioned schemas"

# Regenerate schemas for a single version
regenerate: .compile-generator
ifndef V
	$(error V is required. Usage: make regenerate V=1.47.0)
endif
	@if [ ! -d "okapi-releases/$(V)" ]; then \
		echo "Error: okapi-releases/$(V) does not exist. Use 'make add-release V=$(V)' first."; \
		exit 1; \
	fi
	@echo "Regenerating schemas for Okapi $(V)..."
	@rm -f okapi-releases/$(V)/schemas/*.schema.json
	@mvn -B -q dependency:resolve -Dokapi.version=$(V) 2>/dev/null || true
	@mvn -B -q compile -Dokapi.version=$(V)
	@mvn -B exec:java@generate-schemas -Dexec.args="okapi-releases/$(V)/schemas" -Dokapi.version=$(V)
	@echo '{"okapiVersion":"$(V)","generatedAt":"'$$(date -u +%Y-%m-%dT%H:%M:%SZ)'","filterCount":'$$(ls -1 okapi-releases/$(V)/schemas/*.schema.json 2>/dev/null | wc -l | tr -d ' ')'}' \
		| jq . > okapi-releases/$(V)/schemas/meta.json
	@echo "Regenerated $$(ls -1 okapi-releases/$(V)/schemas/*.schema.json | wc -l | tr -d ' ') schemas"

# Regenerate schemas for all supported versions
regenerate-all: .compile-generator
	@echo "Regenerating schemas for all supported versions..."
	@for version in $$(jq -r '.supported[]' $(VERSIONS_FILE)); do \
		echo ""; \
		echo "=== Okapi $$version ==="; \
		$(MAKE) regenerate V=$$version || exit 1; \
	done
	@echo ""
	@echo "All schemas regenerated."

# ============================================================================
# Versioning
# ============================================================================

# Run SchemaVersioner across all versions to produce final schemas/
version-schemas: .compile-generator
	@echo "Versioning schemas across Okapi releases..."
	@rm -rf $(SCHEMA_OUTPUT)
	@mkdir -p $(SCHEMA_OUTPUT)
	@baseline=$$(jq -r '.schemaBaseline' $(VERSIONS_FILE)); \
	for version in $$(jq -r '.supported[]' $(VERSIONS_FILE)); do \
		echo "Processing Okapi $$version..."; \
		if [ ! -d "okapi-releases/$$version/schemas" ]; then \
			echo "  Warning: okapi-releases/$$version/schemas not found, skipping"; \
			continue; \
		fi; \
		cp okapi-releases/$$version/schemas/*.schema.json $(SCHEMA_OUTPUT)/ 2>/dev/null || true; \
		cp okapi-releases/$$version/schemas/meta.json $(SCHEMA_OUTPUT)/ 2>/dev/null || true; \
		mvn -B -q exec:java@version-schemas -Dexec.args="$$version $(SCHEMA_OUTPUT)" -Dokapi.version=1.47.0; \
	done
	@echo ""
	@echo "Versioned schemas written to $(SCHEMA_OUTPUT)/"
	@echo "Schema versions summary:"
	@jq -r 'to_entries | .[] | "  \(.key): v\(.value.currentVersion) (introduced in \(.value.introducedInOkapi))"' \
		$(SCHEMA_OUTPUT)/schema-versions.json 2>/dev/null | head -20
	@if [ $$(jq 'length' $(SCHEMA_OUTPUT)/schema-versions.json) -gt 20 ]; then \
		echo "  ... and $$(( $$(jq 'length' $(SCHEMA_OUTPUT)/schema-versions.json) - 20 )) more"; \
	fi

# ============================================================================
# Build
# ============================================================================

build:
ifndef V
	$(error V is required. Usage: make build V=1.47.0)
endif
	@echo "Building okapi-bridge for Okapi $(V)..."
	@mvn -B package -Dokapi.version=$(V) -DskipTests

build-releases:
	@echo "Building release binaries..."
	@for version in $$(jq -r '.build[]' $(VERSIONS_FILE)); do \
		echo ""; \
		echo "=== Building for Okapi $$version ==="; \
		mvn -B package -Dokapi.version=$$version -DskipTests || exit 1; \
		cp target/gokapi-bridge-*-jar-with-dependencies.jar \
			target/gokapi-bridge-okapi$$version-jar-with-dependencies.jar; \
	done
	@echo ""
	@echo "Release binaries built."

test:
	@mvn -B test -Dokapi.version=$$(jq -r '.latest' $(VERSIONS_FILE))

clean:
	@mvn -B clean
	@rm -f .compile-generator
	@rm -rf $(SCHEMA_OUTPUT)

# ============================================================================
# Utility
# ============================================================================

# Show differences between schema versions
diff-schemas:
ifndef V1
	$(error V1 and V2 are required. Usage: make diff-schemas V1=1.46.0 V2=1.47.0)
endif
ifndef V2
	$(error V1 and V2 are required. Usage: make diff-schemas V1=1.46.0 V2=1.47.0)
endif
	@echo "Comparing schemas between Okapi $(V1) and $(V2)..."
	@echo ""
	@echo "Filters in $(V1) but not in $(V2):"
	@comm -23 <(ls okapi-releases/$(V1)/schemas/*.schema.json 2>/dev/null | xargs -n1 basename | sort) \
	          <(ls okapi-releases/$(V2)/schemas/*.schema.json 2>/dev/null | xargs -n1 basename | sort) || true
	@echo ""
	@echo "Filters in $(V2) but not in $(V1):"
	@comm -13 <(ls okapi-releases/$(V1)/schemas/*.schema.json 2>/dev/null | xargs -n1 basename | sort) \
	          <(ls okapi-releases/$(V2)/schemas/*.schema.json 2>/dev/null | xargs -n1 basename | sort) || true
