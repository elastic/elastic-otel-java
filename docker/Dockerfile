FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:403b119589037b9f360cafc8571b228f317e05d8731366da82daa2bcabda5d02
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
