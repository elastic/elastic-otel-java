FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:ee0c84cd7ba856d53eef4ab78030b57b8dd3eaaf1a54b1ea35472662c2975b7c
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY --chown=65532:65532 ${JAR_FILE} /javaagent.jar
COPY --chown=65532:65532 ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar
# Use wolfi's "nonroot" user/group to satisfy k8s runAsNonRoot security policies.
USER 65532:65532
RUN chmod go+r /javaagent.jar /extensions
