FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:973431347ad45f40e01afbbd010bf9de929c088a63382239b90dd84f39618bc8
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
