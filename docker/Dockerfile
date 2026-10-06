FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:40515e56512ace084d34fbaa37a89e4e8e5ad5162ef5c922c3c4042f8b74f7b7
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY --chown=65532:65532 ${JAR_FILE} /javaagent.jar
COPY --chown=65532:65532 ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar
# Use wolfi's "nonroot" user/group to satisfy k8s runAsNonRoot security policies.
USER 65532:65532
RUN chmod go+r /javaagent.jar /extensions
