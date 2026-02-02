FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:fff708a5284df216b8949c309b404ed9e89a9c79855a1f5456a157e8d21288b4
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
