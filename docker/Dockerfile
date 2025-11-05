FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:401d8684fd5932c178d7cf524cb05a5088d0e4f74e886e2d0a3b4b5127349a49
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
