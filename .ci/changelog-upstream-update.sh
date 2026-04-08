#!/usr/bin/env bash
set -euo pipefail
# this script creates and pushes a changelog entry for the upstream otel dependencies.
# it relies on the following environment variables:
#   PR_TITLE: pull-request title (used in the changelog title)
#   PR_URL: pull-request URL (used in the changelog entry to link to the PR)
#   PR_NUMBER: pull-request number (used to name the changelog entry file)

description="- $("./gradlew" -q changelogUpstreamDependenciesOneLiner)"
docs-builder changelog add \
  --concise \
  --title "${PR_TITLE}" \
  --type enhancement \
  --prs "${PR_URL}" \
  --products "edot-java NEXT ga" \
  --description "${description}"

# will overwrite any prior update, if there is any
mv -v "./docs/changelog/${PR_NUMBER}.yaml" "./docs/changelog/upstream-update.yaml"

if [[ -z "$(git status --porcelain)" ]]; then
  echo "No changes to commit"
else
  git add --all .
  git commit -m "generate changelog entry"
  git push
fi
