## CI/CD

There are three main stages that run on GitHub actions:

* Build
* Unit Test
* Release

### Scenarios

* Tests should be triggered on branch, tag, and PR basis.
* Commits that are only affecting the docs files should not trigger any test or similar stages that are not required.
* Automated release in the CI gets triggered through a GitHub workflow.
* **This is not the case yet**, but if Github secrets are required, Pull Requests from forked repositories won't run any build accessing those secrets. If needed, create a feature branch (opened directly on the upstream project).

### How do you interact with the CI?

#### On a PR basis

Once a PR has been opened, then there are two different ways you can trigger builds in the CI:

1. Git commit based
2. UI-based, any Elasticians can force a build through the GitHub UI

#### Branches

Whenever a merge to the main or branches, the whole workflow will be compiled and tested on Linux and Windows.

### Release process

This process has been fully automated, and it gets triggered manually when the [release](https://github.com/elastic/elastic-otel-java/actions/workflows/release.yml) workflow is being run. It runs a Buildkite pipeline in charge of generating and publishing the artifacts; for further details, please go to [the Buildkite folder](../../.buildkite/README.md). 

The tag release follows the naming convention: `v.<major>.<minor>.<patch>`, where `<major>`, `<minor>` and `<patch>`.

The release automation raises a Pull Request with the next version.

### OpenTelemetry

A GitHub workflow is responsible for populating the workflows regarding jobs and steps. Those details can be seen [here](https://ela.st/oblt-ci-cd-stats) (**NOTE**: only available for Elasticians).
