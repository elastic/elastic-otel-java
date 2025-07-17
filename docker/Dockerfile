FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:bc3e1ff1495c06129cf27631a5a29d1f0d086cd1ab4fdf5363b5bc11518745c4
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
