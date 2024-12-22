FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:3a6e9134cf3142da74153a522822c8fa56d09376e294627e51db8aa28f5d20d3
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
