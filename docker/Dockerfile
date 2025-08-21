FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:4e1949138de0c3c2bf24ed3d6ce04edd76333ec34e7f27bb64e2fcd648207603
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
