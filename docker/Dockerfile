FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:5ffa55ac0a7ec95f1232622f62d5c0953ca931ae64cfd9946629b0e408a85d0e
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
