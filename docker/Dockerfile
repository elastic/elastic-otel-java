FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:a58ef014ab5946b40bc6bbe5cb5f937d2bdaae3682af6861020c3941ba7a22b7
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
