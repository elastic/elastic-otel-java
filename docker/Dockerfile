FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:442a5663000b3d66d565e61d400b30a4638383a72d90494cfc3104b34dfb3211
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
