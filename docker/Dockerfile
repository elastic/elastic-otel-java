FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:bd401704a162a7937cd1015f755ca9da9aba0fdf967fc6bf90bf8d3f6b2eb557
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
