#!/bin/bash
# Generate pom.xml for a specific Okapi version.
#
# The generated pom inherits from the parent (okapi-bridge-parent) which provides:
#   - All infrastructure dependencies (gRPC, Gson, SnakeYAML, etc.)
#   - Plugin configuration (protobuf, shade, exec, build-helper)
#
# The generated pom declares:
#   - Parent reference
#   - Okapi version + Java version properties
#   - okapi-lib dependency (Okapi's SDK uber-JAR with all filters + steps)
#   - Build section with source paths and plugin references
#
# Using okapi-lib instead of individual filter/step artifacts means we
# automatically get all Okapi components without maintaining artifact lists.
#
# Usage: ./scripts/generate-version-pom.sh [--local] [--output-dir DIR] <version>
#
# Options:
#   --local         Use local Maven repo (~/.m2) to verify okapi-lib exists
#   --output-dir    Write pom.xml to a custom directory (uses absolute path references)

set -e

REPO_ROOT=$(cd "$(dirname "$0")/.." && pwd)

# Parse flags
LOCAL_MODE=false
CUSTOM_OUTPUT_DIR=""
while [[ $# -gt 0 ]]; do
    case "$1" in
        --local)
            LOCAL_MODE=true
            shift
            ;;
        --output-dir)
            CUSTOM_OUTPUT_DIR="$2"
            shift 2
            ;;
        -*)
            echo "Unknown option: $1"
            exit 1
            ;;
        *)
            VERSION="$1"
            shift
            ;;
    esac
done

if [ -z "$VERSION" ]; then
    echo "Usage: $0 [--local] [--output-dir DIR] <okapi-version>"
    exit 1
fi

if [ -n "$CUSTOM_OUTPUT_DIR" ]; then
    OUTPUT_DIR="$CUSTOM_OUTPUT_DIR"
    PATH_PREFIX="$REPO_ROOT/"
else
    OUTPUT_DIR="okapi-releases/$VERSION"
    PATH_PREFIX="../../"
fi
OUTPUT_FILE="$OUTPUT_DIR/pom.xml"
META_FILE="okapi-releases/$VERSION/meta.json"

# Read Java version from meta.json if it exists, default to 11
JAVA_VERSION="11"
if [ -f "$META_FILE" ]; then
    META_JAVA_VERSION=$(jq -r '.javaVersion // "11"' "$META_FILE" 2>/dev/null)
    if [ -n "$META_JAVA_VERSION" ] && [ "$META_JAVA_VERSION" != "null" ]; then
        JAVA_VERSION="$META_JAVA_VERSION"
    fi
fi
echo "Java version for Okapi $VERSION: $JAVA_VERSION"

# Read bridge version from root pom.xml
BRIDGE_VERSION=$(sed -n '/<artifactId>okapi-bridge-parent<\/artifactId>/{n;s/.*<version>\(.*\)<\/version>.*/\1/p;}' "$(dirname "$0")/../pom.xml")
if [ -z "$BRIDGE_VERSION" ]; then
    echo "Error: could not read version from root pom.xml"
    exit 1
fi
echo "Bridge version: $BRIDGE_VERSION"

# Verify okapi-lib is available for this version
echo "Checking okapi-lib availability for Okapi $VERSION..."
if [ "$LOCAL_MODE" = true ]; then
    if [ -f "$HOME/.m2/repository/net/sf/okapi/okapi-lib/$VERSION/okapi-lib-$VERSION.jar" ]; then
        echo "  ✓ okapi-lib $VERSION (local)"
    else
        echo "  ⚠ okapi-lib $VERSION not found locally"
    fi
else
    URL="https://repo1.maven.org/maven2/net/sf/okapi/okapi-lib/$VERSION/okapi-lib-$VERSION.pom"
    if curl --output /dev/null --silent --head --fail --max-time 5 "$URL" 2>/dev/null; then
        echo "  ✓ okapi-lib $VERSION (Maven Central)"
    else
        echo "  ⚠ okapi-lib $VERSION not found on Maven Central"
        echo "    Falling back to okapi-core only (filters/steps may be limited)"
    fi
fi

# Ensure output directory exists
mkdir -p "$OUTPUT_DIR"

# Generate pom.xml — uses okapi-lib for all Okapi components (filters + steps)
cat > "$OUTPUT_FILE" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<!--
    Okapi Bridge - Build for Okapi $VERSION
    Generated: $(date -u +%Y-%m-%dT%H:%M:%SZ)

    Uses okapi-lib (Okapi SDK) which bundles all filters and steps.
    Inherits infrastructure deps (gRPC, Gson, etc.) and plugin config from parent.
-->
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>neokapi</groupId>
        <artifactId>okapi-bridge-parent</artifactId>
        <version>$BRIDGE_VERSION</version>
        <relativePath>${PATH_PREFIX}pom.xml</relativePath>
    </parent>

    <artifactId>neokapi-bridge-okapi-$VERSION</artifactId>
    <packaging>jar</packaging>
    <name>Okapi Bridge - Okapi $VERSION</name>

    <properties>
        <okapi.version>$VERSION</okapi.version>
        <maven.compiler.source>$JAVA_VERSION</maven.compiler.source>
        <maven.compiler.target>$JAVA_VERSION</maven.compiler.target>
    </properties>

    <dependencies>
        <!-- Infrastructure deps (okapi-core, gRPC, Gson, etc.) inherited from parent.
             okapi-core version resolves via \${okapi.version} override above. -->

        <!-- Okapi SDK: bundles all filters + steps for this version -->
        <dependency>
            <groupId>net.sf.okapi</groupId>
            <artifactId>okapi-lib</artifactId>
            <version>\${okapi.version}</version>
        </dependency>
    </dependencies>

    <build>
        <!-- Compile bridge source from bridge-core module -->
        <sourceDirectory>${PATH_PREFIX}bridge-core/src/main/java</sourceDirectory>
        <extensions>
            <extension>
                <groupId>kr.motd.maven</groupId>
                <artifactId>os-maven-plugin</artifactId>
                <version>1.7.1</version>
            </extension>
        </extensions>
        <plugins>
            <!-- Protobuf + gRPC codegen (config inherited from parent) -->
            <plugin>
                <groupId>org.xolstice.maven.plugins</groupId>
                <artifactId>protobuf-maven-plugin</artifactId>
                <configuration>
                    <protoSourceRoot>${PATH_PREFIX}bridge-core/src/main/proto</protoSourceRoot>
                </configuration>
            </plugin>
            <!-- Add schema-generator source for exec:java -->
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>build-helper-maven-plugin</artifactId>
                <executions>
                    <execution>
                        <id>add-tools-source</id>
                        <phase>generate-sources</phase>
                        <goals>
                            <goal>add-source</goal>
                        </goals>
                        <configuration>
                            <sources>
                                <source>${PATH_PREFIX}tools/schema-generator/src/main/java</source>
                            </sources>
                        </configuration>
                    </execution>
                    <execution>
                        <id>add-tools-resources</id>
                        <phase>generate-resources</phase>
                        <goals>
                            <goal>add-resource</goal>
                        </goals>
                        <configuration>
                            <resources>
                                <resource>
                                    <directory>${PATH_PREFIX}schemagen</directory>
                                </resource>
                                <resource>
                                    <directory>\${project.build.directory}/schema-resources</directory>
                                </resource>
                            </resources>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
            <!-- Exec plugin: schema generation (inherited from parent) + schema preparation -->
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>exec-maven-plugin</artifactId>
                <executions>
                    <execution>
                        <id>prepare-schemas</id>
                        <phase>generate-resources</phase>
                        <goals>
                            <goal>exec</goal>
                        </goals>
                        <configuration>
                            <executable>bash</executable>
                            <arguments>
                                <argument>${PATH_PREFIX}scripts/prepare-schemas-for-jar.sh</argument>
                                <argument>\${okapi.version}</argument>
                                <argument>\${project.build.directory}/schema-resources/schemas</argument>
                            </arguments>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
            <!-- Assembly: fat JAR (config inherited from parent) -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
EOF

echo "Generated $OUTPUT_FILE"
