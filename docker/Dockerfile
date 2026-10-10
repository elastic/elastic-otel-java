FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:ac3e46f28be65489e286f4af9244790b9c56e6acfa686a14c77ab365b4796b6b
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY --chown=65532:65532 ${JAR_FILE} /javaagent.jar
COPY --chown=65532:65532 ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar
# Use wolfi's "nonroot" user/group to satisfy k8s runAsNonRoot security policies.
USER 65532:65532
RUN chmod go+r /javaagent.jar /extensions
