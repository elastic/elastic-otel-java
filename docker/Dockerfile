FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:55ee1dca9780931b0929d6eb734f455790c06ddbb59f55008e0cddebfbfd1e2e
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
