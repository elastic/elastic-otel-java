#!/usr/bin/env bash
##  This script runs the snapshot given the different environment variables
##    dry_run
##
##  It relies on the .buildkite/hooks/pre-command so the Vault and other tooling
##  are prepared automatically by buildkite.
##

set -eo pipefail

# Make sure we delete this folder before leaving even in case of failure
clean_up () {
  ARG=$?
  export VAULT_TOKEN=$PREVIOUS_VAULT_TOKEN
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
if [[ "$dry_run" == "true" ]] ; then
    echo "--- Build and publish the snapshot :package: (dry-run)"
    publishArg='publishAllPublicationsToDryRunRepository'
else
    echo "--- Build and publish the snapshot :package:"
    publishArg='publishToSonatype closeAndReleaseStagingRepository'
fi

./gradlew \
    --console=plain \
    clean ${publishArg} \
    | tee snapshot.txt

if [[ "$dry_run" == "true" ]] ; then
    echo "--- Archive the dry-run repository :package: (dry-run)"
    tar czvf ./build/dry-run-maven-repo.tgz -C ./build/dry-run-maven-repo/ . | tee snapshot.txt
fi
