FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:6c2e5acc670ecfc05a736e0bb7ce75a7e9cee0f0d931be67de8f6f99225ca5e4
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY --chown=65532:65532 ${JAR_FILE} /javaagent.jar
COPY --chown=65532:65532 ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar
# Use wolfi's "nonroot" user/group to satisfy k8s runAsNonRoot security policies.
USER 65532:65532
RUN chmod go+r /javaagent.jar /extensions
