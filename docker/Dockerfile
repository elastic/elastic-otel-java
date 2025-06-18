FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:c634d77ea251a2264a8f4009f53315408fb529101d2afcaeaed66f5b4257ccbb
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
