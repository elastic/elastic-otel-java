FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:41e3562f396823f6b24136417bc0db0b8dd75a3354a0a219574afd545e938263
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
