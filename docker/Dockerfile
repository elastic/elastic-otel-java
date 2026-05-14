FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:2ffdfc3341f566c0e67621d0fb6628a4e33c2d2e4c51b682121bc1adbf8eb3f3
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY --chown=65532:65532 ${JAR_FILE} /javaagent.jar
COPY --chown=65532:65532 ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar
# Use wolfi's "nonroot" user/group to satisfy k8s runAsNonRoot security policies.
USER 65532:65532
RUN chmod go+r /javaagent.jar /extensions
