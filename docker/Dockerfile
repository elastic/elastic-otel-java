FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:32099b99697d9da842c1ccacdbef1beee05a68cddb817e858d7656df45ed4c93
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
