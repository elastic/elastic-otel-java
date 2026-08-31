FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:6ef4f1e46fc4c5793d844e416c307a09b5cfd8854049a82110030c13a164a167
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY --chown=65532:65532 ${JAR_FILE} /javaagent.jar
COPY --chown=65532:65532 ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar
# Use wolfi's "nonroot" user/group to satisfy k8s runAsNonRoot security policies.
USER 65532:65532
RUN chmod go+r /javaagent.jar /extensions
