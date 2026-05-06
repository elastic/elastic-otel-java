FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:1ac0a7fe82937eda954a0bbf718047d66df5ed8cf9ababd833cc91946a999c4f
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY --chown=65532:65532 ${JAR_FILE} /javaagent.jar
COPY --chown=65532:65532 ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar
# Use wolfi's "nonroot" user/group to satisfy k8s runAsNonRoot security policies.
USER 65532:65532
RUN chmod go+r /javaagent.jar /extensions
