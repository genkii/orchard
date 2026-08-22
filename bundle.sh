#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SRC_DIR="$SCRIPT_DIR"
CONFIG_DIR="$SCRIPT_DIR/orchard-config"
TEMP_DIR=$(mktemp -d)
OUTPUT_DIR="$SCRIPT_DIR/bundled"

cleanup() { rm -rf "$TEMP_DIR"; }
trap cleanup EXIT

if [ ! -d "$CONFIG_DIR/data" ] || [ ! -d "$CONFIG_DIR/nbt" ]; then
    echo "Error: Config directory not found at $CONFIG_DIR"
    echo "Expected: $CONFIG_DIR/data/ and $CONFIG_DIR/nbt/"
    exit 1
fi

echo "=== Orchard Bundled Build ==="
echo ""

# 1. Copy project to temp
echo "[1/4] Copying project to temp directory..."
cp -r "$SRC_DIR" "$TEMP_DIR/orchard"

# 2. Copy config files into resources
echo "[2/4] Bundling config files..."
RESOURCE_DIR="$TEMP_DIR/orchard/common/src/main/resources/default-config"
mkdir -p "$RESOURCE_DIR/data" "$RESOURCE_DIR/nbt"

cp "$CONFIG_DIR"/data/*.json "$RESOURCE_DIR/data/"
cp "$CONFIG_DIR"/nbt/*.nbt "$RESOURCE_DIR/nbt/"

# 3. Generate manifest
echo "[3/4] Generating manifest..."
MANIFEST="$TEMP_DIR/orchard/common/src/main/resources/default-config/manifest.txt"
: > "$MANIFEST"

for f in "$RESOURCE_DIR"/data/*.json; do
    echo "default-config/data/$(basename "$f")" >> "$MANIFEST"
done
for f in "$RESOURCE_DIR"/nbt/*.nbt; do
    echo "default-config/nbt/$(basename "$f")" >> "$MANIFEST"
done

MANIFEST_COUNT=$(wc -l < "$MANIFEST")
echo "  Bundled $MANIFEST_COUNT file(s) (data + nbt)"

# 4. Build
echo "[4/4] Building Fabric and NeoForge JARs..."
cd "$TEMP_DIR/orchard"

# Append -BUNDLED suffix to version for bundled builds only
VERSION=$(grep '^version=' gradle.properties | cut -d= -f2)
if [[ "$VERSION" != *-BUNDLED ]]; then
    sed -i "s/^version=.*/version=${VERSION}-BUNDLED/" gradle.properties
fi
echo "  Version: ${VERSION%-BUNDLED}-BUNDLED"

./gradlew :fabric:build :neoforge:build --no-daemon -q 2>&1 | tail -5

# 5. Collect output
mkdir -p "$OUTPUT_DIR"
rm -f "$OUTPUT_DIR"/*.jar

FABRIC_JAR=$(find fabric/build/libs -maxdepth 1 -name '*.jar' ! -name '*sources*' ! -name '*javadoc*' | head -1)
NEOFORGE_JAR=$(find neoforge/build/libs -maxdepth 1 -name '*.jar' ! -name '*sources*' ! -name '*javadoc*' | head -1)

if [ -n "$FABRIC_JAR" ]; then
    cp "$FABRIC_JAR" "$OUTPUT_DIR/"
    echo "  Fabric:    $(basename "$FABRIC_JAR")"
fi

if [ -n "$NEOFORGE_JAR" ]; then
    cp "$NEOFORGE_JAR" "$OUTPUT_DIR/"
    echo "  NeoForge:  $(basename "$NEOFORGE_JAR")"
fi

echo ""
echo "=== Done ==="
echo "Output in: $OUTPUT_DIR/"
ls -lh "$OUTPUT_DIR/"*.jar 2>/dev/null || echo "  (no JARs found)"
