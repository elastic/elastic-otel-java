FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:7082adcc2c4380be273ab5b80c4a762b4f17279c13c6fc8f87a60190aee2e2cd
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
