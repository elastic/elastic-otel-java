FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:0a6f6d3cf5a9280ed2a5171e8fe97dbe98fc6f17de2f50d31de3b2168c5402e7
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY --chown=65532:65532 ${JAR_FILE} /javaagent.jar
COPY --chown=65532:65532 ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar
# Use wolfi's "nonroot" user/group to satisfy k8s runAsNonRoot security policies.
USER 65532:65532
RUN chmod go+r /javaagent.jar /extensions
