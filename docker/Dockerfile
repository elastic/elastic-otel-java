FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:e7cba125e50439f1bfd52d327dfbf10994ad8c2556265b30055d7af58c1aa600
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
