FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:9060788fe55e611245d1b4a53ef705018a2703b4879a950a276fe113ef750cd6
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
