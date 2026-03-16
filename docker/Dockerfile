FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:c71dd0b6e7865a87fd7414e9ecaee5d9db988c70c1f5fc0881a11aac2de55627
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
