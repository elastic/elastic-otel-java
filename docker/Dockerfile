FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:8cff240b81057968575dd28dab0c3609657cb7e0e60ff017261e5b721fad9e1b
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
