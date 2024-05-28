FROM docker.elastic.co/wolfi/chainguard-base@sha256:9f940409f96296ef56140bcc4665c204dd499af4c32c96cc00e792558097c3f1
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
