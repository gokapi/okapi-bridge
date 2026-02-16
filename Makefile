# Okapi Bridge Makefile
# Manages schema generation across Okapi versions

SHELL := /bin/bash
.PHONY: help list-upstream list-local add-release regenerate regenerate-all \
        version-schemas build clean test generate-pom generate-all-poms

# Configuration - derived from okapi-releases directory
SUPPORTED_VERSIONS := $(shell ls -1 okapi-releases 2>/dev/null | sort -V)
LATEST_VERSION := $(shell ls -1 okapi-releases 2>/dev/null | sort -V | tail -1)
VERSIONS_FILE := schema-versions.json

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
	@echo "  make version-schemas      Recompute schema versions across all releases"
	@echo ""
	@echo "Dependencies:"
	@echo "  make generate-pom V=1.47.0  Generate version-specific pom.xml"
	@echo "  make generate-all-poms      Generate pom.xml for all versions"
	@echo ""
	@echo "Build:"
	@echo "  make build V=1.47.0       Build JAR for specific Okapi version"
	@echo "  make test                 Run tests"
	@echo "  make clean                Clean build artifacts"
	@echo ""
	@echo "Supported versions: $(SUPPORTED_VERSIONS)"
	@echo "Latest version: $(LATEST_VERSION)"

# ============================================================================
# Discovery
# ============================================================================

list-upstream:
	@echo "Available Okapi versions in Maven Central:"
	@curl -s "https://search.maven.org/solrsearch/select?q=g:net.sf.okapi+AND+a:okapi-core&core=gav&rows=50&wt=json" \
		| jq -r '.response.docs[].v' | sort -V

list-local:
	@echo "Local okapi-releases/ directories:"
	@ls -1 okapi-releases/ 2>/dev/null | grep -E '^[0-9]' | sort -V || echo "  (none)"
	@echo ""
	@echo "Latest version: $(LATEST_VERSION)"

# ============================================================================
# Schema Management
# ============================================================================

# Ensure schema generator is compiled
.compile-generator:
	@echo "Compiling schema generator..."
	@mvn -B -q compile -Dokapi.version=1.47.0
	@touch .compile-generator

# Add a new Okapi release
add-release:
ifndef V
	$(error V is required. Usage: make add-release V=1.48.0)
endif
	@echo "Adding Okapi release $(V)..."
	@mkdir -p okapi-releases/$(V)/schemas
	@mkdir -p okapi-releases/$(V)/overrides
	@# Discover available filters and generate version-specific pom.xml
	@echo "Discovering available filters..."
	@./scripts/generate-version-pom.sh $(V)
	@# Compile with version-specific dependencies
	@echo "Compiling with Okapi $(V) dependencies..."
	@mvn -B -q compile -f okapi-releases/$(V)/pom.xml
	@# Generate schemas using version-specific pom
	@echo "Generating schemas for Okapi $(V)..."
	@mvn -B exec:java@generate-schemas -Dexec.args="okapi-releases/$(V)/schemas" -f okapi-releases/$(V)/pom.xml
	@# Create meta.json
	@echo '{"okapiVersion":"$(V)","generatedAt":"'$$(date -u +%Y-%m-%dT%H:%M:%SZ)'","filterCount":'$$(ls -1 okapi-releases/$(V)/schemas/*.schema.json 2>/dev/null | wc -l | tr -d ' ')'}' \
		| jq . > okapi-releases/$(V)/schemas/meta.json
	@# Run versioner to assign schema versions (uses latest for the versioner code)
	@echo "Assigning schema versions..."
	@mvn -B -q compile -Dokapi.version=$(LATEST_VERSION)
	@mvn -B -q exec:java@version-schemas -Dexec.args="$(V) okapi-releases/$(V)/schemas $(VERSIONS_FILE)" -Dokapi.version=$(LATEST_VERSION)
	@echo ""
	@echo "Created okapi-releases/$(V)/ with $$(ls -1 okapi-releases/$(V)/schemas/*.schema.json | wc -l | tr -d ' ') schemas"
	@echo ""
	@echo "Next steps:"
	@echo "  1. Review generated schemas in okapi-releases/$(V)/schemas/"
	@echo "  2. Optionally copy overrides from previous version"
	@echo "  3. Commit: git add okapi-releases/$(V) $(VERSIONS_FILE)"

# Regenerate schemas for a single version
regenerate:
ifndef V
	$(error V is required. Usage: make regenerate V=1.47.0)
endif
	@if [ ! -d "okapi-releases/$(V)" ]; then \
		echo "Error: okapi-releases/$(V) does not exist. Use 'make add-release V=$(V)' first."; \
		exit 1; \
	fi
	@echo "Regenerating schemas for Okapi $(V)..."
	@rm -f okapi-releases/$(V)/schemas/*.schema.json
	@# Use version-specific pom if available, otherwise generate it
	@if [ ! -f "okapi-releases/$(V)/pom.xml" ]; then \
		echo "Generating pom.xml for Okapi $(V)..."; \
		./scripts/generate-version-pom.sh $(V); \
	fi
	@mvn -B -q compile -f okapi-releases/$(V)/pom.xml
	@mvn -B exec:java@generate-schemas -Dexec.args="okapi-releases/$(V)/schemas" -f okapi-releases/$(V)/pom.xml
	@echo '{"okapiVersion":"$(V)","generatedAt":"'$$(date -u +%Y-%m-%dT%H:%M:%SZ)'","filterCount":'$$(ls -1 okapi-releases/$(V)/schemas/*.schema.json 2>/dev/null | wc -l | tr -d ' ')'}' \
		| jq . > okapi-releases/$(V)/schemas/meta.json
	@echo "Regenerated $$(ls -1 okapi-releases/$(V)/schemas/*.schema.json | wc -l | tr -d ' ') schemas"
	@echo ""
	@echo "Run 'make version-schemas' to update schema versions"

# Generate pom.xml for a version (for existing versions without pom.xml)
generate-pom:
ifndef V
	$(error V is required. Usage: make generate-pom V=1.47.0)
endif
	@./scripts/generate-version-pom.sh $(V)

# Generate pom.xml files for all versions
generate-all-poms:
	@echo "Generating pom.xml for all supported versions..."
	@for version in $(SUPPORTED_VERSIONS); do \
		if [ ! -f "okapi-releases/$$version/pom.xml" ]; then \
			./scripts/generate-version-pom.sh $$version; \
		else \
			echo "okapi-releases/$$version/pom.xml already exists"; \
		fi; \
	done

# Regenerate schemas for all supported versions
regenerate-all:
	@echo "Regenerating schemas for all supported versions..."
	@for version in $(SUPPORTED_VERSIONS); do \
		echo ""; \
		echo "=== Okapi $$version ==="; \
		$(MAKE) regenerate V=$$version || exit 1; \
	done
	@echo ""
	@echo "All schemas regenerated. Run 'make version-schemas' to update versions."

# ============================================================================
# Versioning
# ============================================================================

# Recompute schema versions across all Okapi releases
version-schemas: .compile-generator
	@echo "Computing schema versions across all Okapi releases..."
	@rm -f $(VERSIONS_FILE)
	@for version in $(SUPPORTED_VERSIONS); do \
		echo "Processing Okapi $$version..."; \
		mvn -B -q exec:java@version-schemas \
			-Dexec.args="$$version okapi-releases/$$version/schemas $(VERSIONS_FILE)" \
			-Dokapi.version=$(LATEST_VERSION); \
	done
	@echo ""
	@echo "Schema versions updated in $(VERSIONS_FILE)"
	@echo ""
	@echo "Summary:"
	@jq -r '.filters | to_entries | sort_by(.key) | .[] | "  \(.key): v\(.value.versions[-1].schemaVersion) (\(.value.versions | length) version(s))"' \
		$(VERSIONS_FILE) 2>/dev/null | head -20
	@count=$$(jq '.filters | length' $(VERSIONS_FILE) 2>/dev/null); \
	if [ "$$count" -gt 20 ]; then \
		echo "  ... and $$(( $$count - 20 )) more filters"; \
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

test:
	@mvn -B test -Dokapi.version=$(LATEST_VERSION)

clean:
	@mvn -B clean
	@rm -f .compile-generator

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
