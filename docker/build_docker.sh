#!/usr/bin/env bash

# See full documentation in the "Creating and publishing Docker images" section
# of CONTRIBUTING.md

set -euxo pipefail

if ! command -v docker
then
  echo "ERROR: Building Docker image requires Docker binary to be installed" && exit 1
elif ! docker version
then
  echo "ERROR: Building Docker image requires Docker daemon to be running" && exit 1
fi
readonly RELEASE_VERSION=${1}

readonly SCRIPT_PATH="$( cd "$(dirname "$0")" ; pwd -P )"
readonly PROJECT_ROOT=$SCRIPT_PATH/../
readonly NAMESPACE="observability"

FILE=$(ls -A ${PROJECT_ROOT}agent/build/libs/*.jar | grep -E "elastic-otel-javaagent-[0-9]+.[0-9]+.[0-9]+(-SNAPSHOT)?.jar" )
EXTENSION_FILE=$(ls -A ${PROJECT_ROOT}agentextension/build/libs/*.jar | grep -E "elastic-otel-agentextension-[0-9]+.[0-9]+.[0-9]+(-SNAPSHOT)?.jar" )

if [ -n "${FILE}" ]
then
  # We have build files to use
  echo "INFO: Found local build artifact agent jar. Using locally built for Docker build"
  cp "${FILE}" "${PROJECT_ROOT}elastic-otel-javaagent.jar" || echo "INFO: No locally built image found"
elif [ ! -z ${SONATYPE_FALLBACK+x} ]
then
  echo "INFO: No local build artifact and SONATYPE_FALLBACK. Falling back to downloading artifact from Sonatype Nexus repository for version $RELEASE_VERSION"
  if ! command -v curl
  then
      echo "ERROR: Pulling images from Sonatype Nexus repo requires cURL to be installed" && exit 1
  fi
  curl -L -s -o elastic-otel-javaagent.jar \
    "https://oss.sonatype.org/service/local/artifact/maven/redirect?r=releases&g=co.elastic.otel&a=elastic-otel-javaagent&v=$RELEASE_VERSION"
  else
    echo "ERROR: No suitable build artifact was found. Re-running this script with the SONATYPE_FALLBACK variable set to true will try to use the Sonatype artifact for the latest tag"
    exit 1
fi

if [ -n "${EXTENSION_FILE}" ]
then
  # We have build files to use
  echo "INFO: Found local build artifact extension jar. Using locally built for Docker build"
  cp "${EXTENSION_FILE}" "${PROJECT_ROOT}elastic-otel-agentextension.jar" || echo "INFO: No locally built image found"
elif [ ! -z ${SONATYPE_FALLBACK+x} ]
then
  echo "INFO: No local build artifact and SONATYPE_FALLBACK. Falling back to downloading artifact from Sonatype Nexus repository for version $RELEASE_VERSION"
  if ! command -v curl
  then
      echo "ERROR: Pulling images from Sonatype Nexus repo requires cURL to be installed" && exit 1
  fi
  curl -L -s -o elastic-otel-agentextension.jar \
    "https://oss.sonatype.org/service/local/artifact/maven/redirect?r=releases&g=co.elastic.otel&a=elastic-otel-agentextension&v=$RELEASE_VERSION"
  else
    echo "ERROR: No suitable build artifact was found. Re-running this script with the SONATYPE_FALLBACK variable set to true will try to use the Sonatype artifact for the latest tag"
    exit 1
fi

ls -l elastic-otel-javaagent.jar elastic-otel-agentextension.jar

echo "INFO: Starting Docker build for version $RELEASE_VERSION"
for DOCKERFILE in "${PROJECT_ROOT}docker/Dockerfile" ; do
  DOCKER_TAG=$RELEASE_VERSION
  docker build -t docker.elastic.co/$NAMESPACE/elastic-otel-javaagent:$DOCKER_TAG \
    --platform linux/amd64 \
    --build-arg JAR_FILE=elastic-otel-javaagent.jar \
    --build-arg EXTENSION_JAR_FILE=elastic-otel-agentextension.jar \
    --file $DOCKERFILE .

  if [ $? -eq 0 ]
  then
    echo "INFO: Docker image built successfully"
  else
    echo "ERROR: Problem building Docker image!"
  fi
done

function finish {

  if [ -f elastic-otel-javaagent.jar ]
  then
    echo "INFO: Cleaning up downloaded artifact"
    rm elastic-otel-javaagent.jar elastic-otel-agentextension.jar
  fi
}

trap finish EXIT