FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:0b3bec7b00c7d38cf2bcb461c98b385de8a8e29c45d3643209d1590371a7f954
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
