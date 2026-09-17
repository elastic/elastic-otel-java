FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:d5c570f7ce6708b39e404390a73c6e385c7a85310a9361ebb371b73a2e4c2848
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY --chown=65532:65532 ${JAR_FILE} /javaagent.jar
COPY --chown=65532:65532 ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar
# Use wolfi's "nonroot" user/group to satisfy k8s runAsNonRoot security policies.
USER 65532:65532
RUN chmod go+r /javaagent.jar /extensions
