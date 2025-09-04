FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:bb3bb9491f564296956a879033da287961829c431fc8dc68222c2b60f9bfa052
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
