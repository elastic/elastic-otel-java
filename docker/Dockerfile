FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:c3abc7aed6b4123b39cb7d0855fd083052ce17f1b3ab852f2fd6c9dee83f4577
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY --chown=65532:65532 ${JAR_FILE} /javaagent.jar
COPY --chown=65532:65532 ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar
# Use wolfi's "nonroot" user/group to satisfy k8s runAsNonRoot security policies.
USER 65532:65532
RUN chmod go+r /javaagent.jar /extensions
