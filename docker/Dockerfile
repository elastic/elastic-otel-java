FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:2b179e1fe69c672bd0844147c6ebb039adb44ddaa3f9b4695f4915a9447da438
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
