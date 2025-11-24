FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:25397820ef168d951339e69cedb5460f354cea842e2af367e33b89e988c51615
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
