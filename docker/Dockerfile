FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:780990eb5ebbbbcf2927b465a4b206b961ea112b18784b92259e2a3109032bd4
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
