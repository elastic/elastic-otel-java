FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:b55a147b9bc64d7ac0ee5ba03ec0565c9613d67a4e9c6fb1eb4b0ed1693daeb7
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY --chown=65532:65532 ${JAR_FILE} /javaagent.jar
COPY --chown=65532:65532 ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar
# Use wolfi's "nonroot" user/group to satisfy k8s runAsNonRoot security policies.
USER 65532:65532
RUN chmod go+r /javaagent.jar /extensions
