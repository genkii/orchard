#!/bin/bash
set -euo pipefail

# Builds Orchard distribution JARs.
#
#   ./bundle.sh          -> full JARs (built-in defaults included) + -TINY JARs
#   ./bundle.sh --tiny   -> only the -TINY JARs (no built-in defaults inside)
#
# The built-in defaults live under orchard-config/ in this repository:
#   orchard-config/data/*.yaml
#   orchard-config/nbt/*.nbt
#
# They are copied into the JAR resources as `default-config/` together with a
# manifest so the mod can extract them into `config/orchard/bundled/` on first
# run (plain data/ + nbt/, not a pack). -TINY builds simply ship without those
# resources.

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SRC_DIR="$SCRIPT_DIR"
CONFIG_DIR="$SCRIPT_DIR/orchard-config"
TEMP_DIR=$(mktemp -d)
OUTPUT_DIR="$SCRIPT_DIR/bundled"

ONLY_TINY=false
if [ "${1:-}" = "--tiny" ]; then
    ONLY_TINY=true
fi

cleanup() { rm -rf "$TEMP_DIR"; }
trap cleanup EXIT

if [ ! -d "$CONFIG_DIR/data" ] || [ ! -d "$CONFIG_DIR/nbt" ]; then
    echo "Error: Config directory not found at $CONFIG_DIR"
    echo "Expected: $CONFIG_DIR/data/ and $CONFIG_DIR/nbt/"
    exit 1
fi

echo "=== Orchard Bundle Build ==="
echo ""

# Never leave JARs of previous runs/versions in the output directory.
mkdir -p "$OUTPUT_DIR"
rm -f "$OUTPUT_DIR"/*.jar

echo "[1/4] Copying project to temp directory..."
cp -r "$SRC_DIR" "$TEMP_DIR/orchard"

copy_default_config() {
    local project_dir="$1"
    local resource_dir="$project_dir/common/src/main/resources/default-config"

    mkdir -p "$resource_dir/data" "$resource_dir/nbt"
    cp "$CONFIG_DIR"/data/*.yaml "$resource_dir/data/"
    cp "$CONFIG_DIR"/nbt/*.nbt "$resource_dir/nbt/"

    # Manifest lists every file relative to the resources root because Java
    # classloaders cannot enumerate directory contents.
    : > "$resource_dir/manifest.txt"
    for f in "$resource_dir"/data/*.yaml; do
        echo "default-config/data/$(basename "$f")" >> "$resource_dir/manifest.txt"
    done
    for f in "$resource_dir"/nbt/*.nbt; do
        echo "default-config/nbt/$(basename "$f")" >> "$resource_dir/manifest.txt"
    done
}

remove_default_config() {
    rm -rf "$1/common/src/main/resources/default-config"
}

build() {
    (cd "$1" && ./gradlew :fabric:build :neoforge:build --no-daemon --build-cache -q 2>&1 | tail -5)
}

collect() {
    local dest="$1"
    mkdir -p "$dest"
    find "$TEMP_DIR/orchard/fabric/build/libs" "$TEMP_DIR/orchard/neoforge/build/libs" \
        -maxdepth 1 -name '*.jar' ! -name '*sources*' ! -name '*javadoc*' \
        -exec cp {} "$dest/" \;
}

VERSION=$(grep '^version=' "$SCRIPT_DIR/gradle.properties" | cut -d= -f2)
echo "  Version: $VERSION"

if [ "$ONLY_TINY" = false ]; then
    echo "[2/4] Bundling built-in defaults..."
    copy_default_config "$TEMP_DIR/orchard"
    MANIFEST_COUNT=$(grep -c . "$TEMP_DIR/orchard/common/src/main/resources/default-config/manifest.txt")
    echo "  Bundled $MANIFEST_COUNT file(s) (data + nbt)"

    echo "[3/4] Building full Fabric and NeoForge JARs..."
    build "$TEMP_DIR/orchard"
    collect "$OUTPUT_DIR"
else
    echo "[2/4] Skipping built-in defaults (--tiny)."
    echo "[3/4] Skipped."
fi

echo "[4/4] Building -TINY JARs (without built-in defaults)..."
remove_default_config "$TEMP_DIR/orchard"
(cd "$TEMP_DIR/orchard" && ./gradlew clean :fabric:build :neoforge:build --no-daemon --build-cache -q 2>&1 | tail -5)

mkdir -p "$OUTPUT_DIR"
for jar in $(find "$TEMP_DIR/orchard/fabric/build/libs" "$TEMP_DIR/orchard/neoforge/build/libs" \
        -maxdepth 1 -name '*.jar' ! -name '*sources*' ! -name '*javadoc*'); do
    name="$(basename "$jar")"
    base="${name%.jar}"
    if [[ "$base" != *-tiny ]]; then
        mv "$jar" "$(dirname "$jar")/${base}-tiny.jar"
    fi
done
collect "$OUTPUT_DIR"

echo ""
echo "=== Done ==="
echo "Output in: $OUTPUT_DIR/"
ls -lh "$OUTPUT_DIR/"*.jar 2>/dev/null || echo "  (no JARs found)"
