# Buildkite

This README provides an overview of the Buildkite pipeline to automate the build and publishing process.

## Release pipeline

The Buildkite pipeline is for building and publishing releases. It will create a Pull Request
with the next version.


## Snapshot pipeline

The Buildkite pipeline is for building and publishing snapshots.

### Pipeline Configuration

To view the pipeline and its configuration, click [here](https://buildkite.com/elastic/elastic-otel-java-snapshot) or
go to the definition in the `elastic/ci` repository.

### Credentials

The release team provides the credentials required to publish the artifacts in Maven Central and sign them
with the GPG.

If further details are needed, please go to [prepare-release.sh](hooks/prepare-release.sh).
