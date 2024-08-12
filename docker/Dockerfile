FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:dcbcd1932d7180cacc83add309cec026aa75dadc1f2c6a2238db5b41dd100b15
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
