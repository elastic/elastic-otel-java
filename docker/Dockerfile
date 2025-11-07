FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:4d7c143ef536d7527d79ef7f27dd5c0e4295b26aa7533e1186d3662a8a1909e2
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
