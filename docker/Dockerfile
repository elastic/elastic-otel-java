FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:bf163e1977002301f7b9fd28fe6837a8cb2dd5c83e4cd45fb67fb28d15d5d40f
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
