FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:0c0ebb28dc0e5fb66326bf170a6dc60a9c6e14ad90ec21f0d4df4bf36bb492b7
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY --chown=65532:65532 ${JAR_FILE} /javaagent.jar
COPY --chown=65532:65532 ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar
# Use wolfi's "nonroot" user/group to satisfy k8s runAsNonRoot security policies.
USER 65532:65532
RUN chmod go+r /javaagent.jar /extensions
