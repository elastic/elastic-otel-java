FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:5225ad181a3e0377c9ee8c6aea868b50ddc9f4b3b9c15e03edd88a3941a2ed21
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY --chown=65532:65532 ${JAR_FILE} /javaagent.jar
COPY --chown=65532:65532 ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar
# Use wolfi's "nonroot" user/group to satisfy k8s runAsNonRoot security policies.
USER 65532:65532
RUN chmod go+r /javaagent.jar /extensions
