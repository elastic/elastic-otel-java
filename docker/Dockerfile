FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:775e340540cbf2fbf81f406f0191824f3c691f2613d8d51da1f992eb26aec82b
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
