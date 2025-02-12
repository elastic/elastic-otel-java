FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:ecd940be9f342ee6173397c48f3df5bb410e95000f8726fd01759b6c39b0beda
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
