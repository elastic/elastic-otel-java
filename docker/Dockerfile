FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:b5a03b6a754fa2f9a29e44e316a6c4df1f606cd8a3cd8810ce3d6a3a3134428b
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
