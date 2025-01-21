FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:ea157dd3d70787c6b6dc9e14dda1ff103c781d4c3f9a544393ff4583dd80c9d0
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
