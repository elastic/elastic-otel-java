FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:a6f1b7fa63adf228802cb89b804b40f1d77a3d88d694c9a3a84c1e289fd5c71f
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY --chown=65532:65532 ${JAR_FILE} /javaagent.jar
COPY --chown=65532:65532 ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar
# Use wolfi's "nonroot" user/group to satisfy k8s runAsNonRoot security policies.
USER 65532:65532
RUN chmod go+r /javaagent.jar /extensions
