#!/usr/bin/env bash
set -euo pipefail
# this script generates the changelog bundle description content

echo "This release is based on the following upstream versions:"
./gradlew -q printUpstreamDependenciesMarkdown
