FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:868f3eb5022ea666a64037370ece2c08a9e00bf2c8e272d0db54232d30d460c2
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
