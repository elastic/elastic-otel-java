#!/bin/env bash
# This script generates changelog entry for upstream dependencies update PR using docs-builder
#
# If there is more than one upstream update, the latest will overwrite the prior one, hence making
# sure that there is one per release.
#
# Usage: .ci/update-upstream-changelog.sh [pr_number]
# Where pr_number is the PR number of an upstream dependencies update PR.
# If pr_number is not provided, it will default to the current PR using github CLI.
pr=${1:-}

pr_url="$(gh pr view ${pr} --json url --jq .url)"
pr_id="${pr_url##*/}"
pr_title="$(gh pr view ${pr} --json title --jq .title)"

folder="$(dirname "${0}")"
temp_dir="$(mktemp -d)"
docs-builder changelog add \
  --concise \
  --title "${pr_title}"\
  --type enhancement \
  --prs "${pr_url}" \
  --products "edot-java NEXT ga" \
  --output "${temp_dir}" \
  --description "- $("${folder}/../gradlew" -q changelogUpstreamDependenciesOneLiner)"

# will overwrite any prior update, if there is any
mv "${temp_dir}/${pr_id}.yaml" "${folder}/../docs/changelog/upstream-update.yaml"
rm -rf "${temp_dir}"
