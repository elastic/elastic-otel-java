FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:aef9a80b65c721485588a6d925ae71652f12e26a4adb66d55510e9e2e4cce645
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
