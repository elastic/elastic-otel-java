#!/usr/bin/env bash

set -euxo pipefail

readonly RELEASE_VERSION=${1}

readonly SCRIPT_PATH="$( cd "$(dirname "$0")" ; pwd -P )"
readonly PROJECT_ROOT=$SCRIPT_PATH/../

FILE=$(ls -A ${PROJECT_ROOT}agent/build/libs/*.jar | grep -E "elastic-otel-javaagent-[0-9]+.[0-9]+.[0-9]+(-SNAPSHOT)?.jar" )
EXTENSION_FILE=$(ls -A ${PROJECT_ROOT}agentextension/build/libs/*.jar | grep -E "elastic-otel-agentextension-[0-9]+.[0-9]+.[0-9]+(-SNAPSHOT)?.jar" )
echo "INFO found '$FILE' and '$EXTENSION_FILE'"

if [ -n "${FILE}" ] && [ -n "${EXTENSION_FILE}" ]
then
  # We have build files to use
  echo "INFO: Found local build artifact agent jars. Copying locally built for Docker build"
  cp "${FILE}" "${PROJECT_ROOT}elastic-otel-javaagent.jar" || echo "INFO: No locally built image found"
  cp "${EXTENSION_FILE}" "${PROJECT_ROOT}elastic-otel-agentextension.jar" || echo "INFO: No locally built image found"
else
  echo "INFO: No local build artifact. Falling back to downloading artifact from Sonatype Nexus repository for version $RELEASE_VERSION"
  if ! command -v curl
  then
      echo "ERROR: Pulling images from Sonatype Nexus repo requires cURL to be installed" && exit 1
  fi
  curl -L -s -o elastic-otel-agentextension.jar \
    "https://oss.sonatype.org/service/local/artifact/maven/redirect?r=releases&g=co.elastic.otel&a=elastic-otel-agentextension&v=$RELEASE_VERSION"
  curl -L -s -o elastic-otel-javaagent.jar \
    "https://oss.sonatype.org/service/local/artifact/maven/redirect?r=releases&g=co.elastic.otel&a=elastic-otel-javaagent&v=$RELEASE_VERSION"
fi

ls -l elastic-otel-javaagent.jar elastic-otel-agentextension.jar

