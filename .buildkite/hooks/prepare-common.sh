#!/usr/bin/env bash
set -euo pipefail

retry() {
    local retries=$1
    shift

    local count=0
    until "$@"; do
        exit=$?
        wait=$((2 ** count))
        count=$((count + 1))
        if [ $count -lt "$retries" ]; then
            >&2 echo "Retry $count/$retries exited $exit, retrying in $wait seconds..."
            sleep $wait
        else
            >&2 echo "Retry $count/$retries exited $exit, no more retries left."
            return $exit
        fi
    done
    return 0
}

echo "--- Install JDK17 :java:"
# JDK version is defined in two different locations, here and .github/workflows/build.yml
JAVA_URL=https://jvm-catalog.elastic.co/jdk
JAVA_HOME=$(pwd)/.openjdk17
JAVA_PKG="$JAVA_URL/latest_openjdk_17_linux.tar.gz"
curl -L --output /tmp/jdk.tar.gz "$JAVA_PKG"
mkdir -p "$JAVA_HOME"
tar --extract --file /tmp/jdk.tar.gz --directory "$JAVA_HOME" --strip-components 1

export JAVA_HOME
PATH=$JAVA_HOME/bin:$PATH
export PATH

java -version || true

echo "--- Install Gradle :gradle:"
retry 5 ./gradlew tasks --quiet
