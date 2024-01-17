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

pulishArg=''
if [[ "$dry_run" == "true" ]] ; then
    echo "--- Build and publish the snapshot :package: (dry-run)"
else
    echo "--- Build and publish the snapshot :package:"
    publishArg='publishAllPublicationsToSnapshotsRepository'
fi

folder="$(readlink -f $(dirname $0))"

# make sure gpg does not warns about permissions
chmod 700 ${folder}/gpg
chmod 600 ${folder}/gpg/gpg.conf

./gradlew --console=plain -Psigning.gnupg.optionsFile=${folder}/gpg/gpg.conf clean ${publishArg} | tee snapshot.txt
