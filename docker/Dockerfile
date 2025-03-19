FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:c4e10ecf3d8a21cf4be2fb53a2f522de50e14c80ce1da487e3ffd13f4d48d24d
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
