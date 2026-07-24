#!/bin/bash

set -euo pipefail

if [ -z "${1:-}" ]; then
    echo "Usage: $0 <version>"
    echo "Example: $0 0.0.3-SNAPSHOT"
    exit 1
fi

VERSION="$1"

if [[ ! "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[a-zA-Z0-9]+)?$ ]]; then
    echo "Error: Version '$VERSION' does not look like a valid semver (e.g. 1.2.3 or 1.2.3-SNAPSHOT)"
    exit 1
fi

sed -i "s/^mod_version=.*/mod_version=${VERSION}/" gradle.properties
echo "Version updated to $VERSION"

if [ -d .git ]; then
    echo "Run 'git add gradle.properties && git commit -m \"Bump version to $VERSION\"' to commit."
fi
