FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:cd0d822f7fd85fb7de9c29936cffb8e31b1d77e2470f2e76b7f4ab06777e0e03
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY --chown=65532:65532 ${JAR_FILE} /javaagent.jar
COPY --chown=65532:65532 ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar
# Use wolfi's "nonroot" user/group to satisfy k8s runAsNonRoot security policies.
USER 65532:65532
RUN chmod go+r /javaagent.jar /extensions
