#!/usr/bin/env bash
##  This script runs the release given the different environment variables
##    dry_run
##
##  It relies on the .buildkite/hooks/pre-command so the Vault and other tooling
##  are prepared automatically by buildkite.
##

set -eo pipefail

# Make sure we delete this folder before leaving even in case of failure
clean_up () {
  ARG=$?
  echo "--- Deleting tmp workspace"
  rm -rf $TMP_WORKSPACE
  exit $ARG
}
trap clean_up EXIT

echo "--- JDK installation info :coffee:"
echo $JAVA_HOME
echo $PATH
java -version

publishArg=''
if [[ "$dry_run" == "false" ]] ; then
    echo "--- Build and publish the release :package:"
    publishArg='publishToSonatype closeAndReleaseStagingRepositories'
else
    echo "--- Build and publish the release :package: (dry-run)"
    publishArg='publishAllPublicationsToDryRunRepository'
fi

./gradlew \
    --console=plain \
    clean ${publishArg} \
    | tee release.txt

if [[ "$dry_run" == "true" ]] ; then
    echo "--- Archive the dry-run repository :package: (dry-run)"
    tar czvf ./build/dry-run-maven-repo.tgz -C ./build/dry-run-maven-repo/ . | tee release.txt
fi

echo "--- Archive the build folders with jar files"
find . -type d -name build -exec find {} -name '*.jar' -print0 \; | xargs -0 tar -cvf "${TARBALL_FILE:-dist.tar}"
