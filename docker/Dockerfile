FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:125b1a962808a017d920c871e557e65e47ecb7d3f87fd16e9ea538fe9dc05a95
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
