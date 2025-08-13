FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:3b2026fffccfc6223a9f49f09279e044008b64e30b948cee17f9a74143a2c850
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
