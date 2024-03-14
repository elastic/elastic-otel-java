#!/usr/bin/env bash

set -euo pipefail

upstreamRef="${1:-main}"

upstream_base_url="https://raw.githubusercontent.com/open-telemetry/opentelemetry-java-instrumentation/${upstreamRef}"
project_root="$(dirname "${0}")/.."

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

upstreamAgentVersion="$(upstream_version 'version.gradle.kts' stableVersion)"
upstreamAgentAlphaVersion="$(upstream_version 'version.gradle.kts' alphaVersion)"
upstreamContribVersion="$(upstream_version 'dependencyManagement/build.gradle.kts' otelContribVersion)"

sed -i "s/^opentelemetryJavaagent = \".*/opentelemetryJavaagent = \"${upstreamAgentVersion}\"/" "${project_root}/gradle/libs.versions.toml"
sed -i "s/^opentelemetryJavaagentAlpha = \".*/opentelemetryJavaagentAlpha = \"${upstreamAgentAlphaVersion}\"/" "${project_root}/gradle/libs.versions.toml"
sed -i "s/^opentelemetryContribAlpha = \".*/opentelemetryContribAlpha = \"${upstreamContribVersion}\"/" "${project_root}/gradle/libs.versions.toml"

