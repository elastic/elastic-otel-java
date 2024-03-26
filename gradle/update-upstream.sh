#!/usr/bin/env bash

set -euo pipefail

upstreamRef="${1:-main}"

upstream_base_url="https://raw.githubusercontent.com/open-telemetry/opentelemetry-java-instrumentation/${upstreamRef}"
folder="$(dirname "${0}")"

upstream_version() {
    set +e
    curl -s "${upstream_base_url}/${1}" \
        | grep "val.*${2}.*=" \
        | sed 's/.*=//' \
        | tr -d '"' \
        | tr -d '[:blank:]' \
        || echo 'unknown'
    set -e
}

upstreamAgentAlphaVersion="$(upstream_version 'version.gradle.kts' alphaVersion)"
upstreamContribVersion="$(upstream_version 'dependencyManagement/build.gradle.kts' otelContribVersion)"

sed -i '' -e "s/^opentelemetryJavaagentAlpha = \".*/opentelemetryJavaagentAlpha = \"${upstreamAgentAlphaVersion}\"/" "${folder}/libs.versions.toml"
sed -i '' -e "s/^opentelemetryContribAlpha = \".*/opentelemetryContribAlpha = \"${upstreamContribVersion}\"/" "${folder}/libs.versions.toml"

