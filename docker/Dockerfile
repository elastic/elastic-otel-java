FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:7baac74b00ab274832cfc4fa2dfa0988a7a410acbb8e783ac29e768ac7d9fbf7
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY --chown=65532:65532 ${JAR_FILE} /javaagent.jar
COPY --chown=65532:65532 ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar
# Use wolfi's "nonroot" user/group to satisfy k8s runAsNonRoot security policies.
USER 65532:65532
RUN chmod go+r /javaagent.jar /extensions
