FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:08fb3e1534f50aac94d6347dc7cb62b68a8c05ad90bf63e42999cc359a62e017
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
