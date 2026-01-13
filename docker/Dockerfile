FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:4339be0d8e06adb8d53f780ff328e4de26a611ba77434a386d5325e22d40e5c4
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
