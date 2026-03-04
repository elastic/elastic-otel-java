FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:6cae7f6f358f71e0030af9e30805333f05dc5bb297bbd083a345d8aed37b4bbb
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
