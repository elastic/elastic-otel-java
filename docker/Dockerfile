FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:4a82c706003370964df94913adfc47aab9a55b4fb2490260e7f7ebcf27cc4240
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
