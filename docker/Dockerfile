FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:4af6df9cf5b7db760c361d8a9b41351b3ac1e97d0a21ac0a5b9c309567b7e90e
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
