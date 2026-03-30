#!/bin/bash -x

set -euo pipefail

add_pr(){
    version="${2}"
    pr="${1}"

    # TODO: do we have to add complete PR URLs here or could we just use the numeric ID ?
    # having the complete URL in the generated changelog entry is however convenient for humans to open the link
    docs-builder changelog add --prs https://github.com/elastic/elastic-otel-java/pull/${pr} --products "edot-java ${version} ga"

    # remove comments and empty lines from generated changelog to reduce noise
    # TODO: might be better to move doc to the bottom or allow to not include comments
    sed -i 's/#.*//g' "./docs/changelog/${pr}.yaml"
    sed -i '/^[[:space:]]*$/d' "./docs/changelog/${pr}.yaml"

    # TODO: to filter duplicate upstream dependency updates, we could maybe just remove any changelog entry
    # for such update, the challenge might be to define a reliable heuristic, relying on PR title seems quite brittle.
}

create_bundle(){
  version="${1}"
  docs-builder changelog bundle \
    --input-products "edot-java ${version} ga" \
    --output "./docs/releases/${version}.yaml"

  # TODO: document changelog remove
  docs-builder changelog remove \
    --products "edot-java ${version} ga"
}

# remove all changelog entries and generated bundles
rm -f ./docs/changelog/*.yaml
rm -f ./docs/releases/*.yaml

# generate changelog and bundles for each release, the PRs have been labelled
add_pr 973 1.10.0
add_pr 1001 1.10.0
add_pr 1017 1.10.0
create_bundle 1.10.0

add_pr 911 1.9.0
add_pr 932 1.9.0
add_pr 958 1.9.0
create_bundle 1.9.0

add_pr 898 1.8.0
add_pr 899 1.8.0
create_bundle 1.8.0

add_pr 838 1.7.0
add_pr 835 1.7.0
add_pr 848 1.7.0
add_pr 844 1.7.0
create_bundle 1.7.0

add_pr 803 1.6.0
add_pr 763 1.6.0
add_pr 789 1.6.0
add_pr 818 1.6.0
create_bundle 1.6.0

add_pr 726 1.5.0
add_pr 641 1.5.0
add_pr 712 1.5.0
add_pr 729 1.5.0
# add changelog entry not related to a PR
# TODO: how should we handle adding a 'tech preview' feature in a GA product ?
docs-builder changelog add \
  --products "edot-java 1.5.0 ga" \
  --type enhancement \
  --title 'tech preview release of central configuration support for dynamically changing instrumentation and sending, using OpAMP protocol'
create_bundle 1.5.0

add_pr 610 1.4.1
create_bundle 1.4.1

add_pr 583 1.4.0
add_pr 593 1.4.0
add_pr 607 1.4.0
add_pr 596 1.4.0
add_pr 580 1.4.0
add_pr 603 1.4.0
create_bundle 1.4.0

add_pr 531 1.3.0
add_pr 564 1.3.0
add_pr 539 1.3.0
add_pr 561 1.3.0
create_bundle 1.3.0

add_pr 514 1.2.1
create_bundle 1.2.1

add_pr 422 1.2.0
add_pr 471 1.2.0
add_pr 474 1.2.0
add_pr 497 1.2.0
add_pr 500 1.2.0
create_bundle 1.2.0

add_pr 423 1.1.0
add_pr 455 1.1.0
create_bundle 1.1.0

# add changelog entry not related to a PR
docs-builder changelog add \
  --products "edot-java 1.0.0 ga" \
  --type enhancement \
  --title 'first GA release'
create_bundle 1.0.0
