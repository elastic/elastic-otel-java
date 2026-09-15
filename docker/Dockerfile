FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:a4e6bb74199419ada6a0c0cdfc0c0fe05a34e733b0fc9054a070c1743ab5c3ed
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY --chown=65532:65532 ${JAR_FILE} /javaagent.jar
COPY --chown=65532:65532 ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar
# Use wolfi's "nonroot" user/group to satisfy k8s runAsNonRoot security policies.
USER 65532:65532
RUN chmod go+r /javaagent.jar /extensions
