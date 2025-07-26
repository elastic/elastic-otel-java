FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:b9d4f5310ebccf219efb74aaa7921d07bffdca8655e9878fccf633448e38d654
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
