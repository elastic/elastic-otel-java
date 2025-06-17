FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:a7acf020605f7b3faf04b180bf68d170e394d1f5b2249fce3805fd5d3f145c0e
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
