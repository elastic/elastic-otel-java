FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:035a2593c6d26cc96f5ca5cfd48fe9bd0930f7472a950f05210f3f98a2eb2fa4
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY --chown=65532:65532 ${JAR_FILE} /javaagent.jar
COPY --chown=65532:65532 ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar
# Use wolfi's "nonroot" user/group to satisfy k8s runAsNonRoot security policies.
USER 65532:65532
RUN chmod go+r /javaagent.jar /extensions
