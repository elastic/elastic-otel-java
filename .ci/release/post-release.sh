#!/usr/bin/env bash

set -euo pipefail

RELATIVE_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
BASE_PROJECT="$(dirname "$(dirname "${RELATIVE_DIR}")")"

BASE_URL="https://repo1.maven.org/maven2/co/elastic/otel/elastic-otel-javaagent"
CF_FILE="${BASE_PROJECT}/cloudfoundry/index.yml"

if [ -z "${RELEASE_VERSION:-}" ]; then
  >&2 echo "The environment variable 'RELEASE_VERSION' isn't defined"
  exit 1
fi
if [[ ! "${RELEASE_VERSION}" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
  >&2 echo "The environment variable 'RELEASE_VERSION' should respect SemVer format"
  exit 1
fi

# Make the script idempotent if the release is already in the Cloud Foundry index.
if grep -e "^${RELEASE_VERSION}:" "${CF_FILE}" >/dev/null; then
  exit 0
fi

echo "Update Cloud Foundry version index"
echo "${RELEASE_VERSION}: ${BASE_URL}/${RELEASE_VERSION}/elastic-otel-javaagent-${RELEASE_VERSION}.jar" >> "${CF_FILE}"
