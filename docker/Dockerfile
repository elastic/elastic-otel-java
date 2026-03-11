FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:4156e0c07f13e14e4ee2387cfb65092df310f639aea9316be4574984807c31da
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
