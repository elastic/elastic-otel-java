FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:ec172778d432abfcd18641ec25270b2fff7c22ff9956d44c495860cdb0477357
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
