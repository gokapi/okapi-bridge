# Okapi Bridge Makefile
# Manages schema generation across Okapi versions

SHELL := /bin/bash
.PHONY: help list-upstream list-local add-release regenerate regenerate-all build clean test

# Configuration - derived from okapi-releases directory
SUPPORTED_VERSIONS := $(shell ls -1 okapi-releases 2>/dev/null | sort -V)
LATEST_VERSION := $(shell ls -1 okapi-releases 2>/dev/null | sort -V | tail -1)

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
	@for version in $(SUPPORTED_VERSIONS); do \
		echo ""; \
		echo "=== Okapi $$version ==="; \
		$(MAKE) regenerate V=$$version || exit 1; \
	done
	@echo ""
	@echo "All schemas regenerated."

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
