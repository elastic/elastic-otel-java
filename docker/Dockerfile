FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:1c4caa90ee9cf26c9143e44074f50ba9bb17636823bde4397751a5e0d846bfd6
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
