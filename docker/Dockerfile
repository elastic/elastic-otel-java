FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:bbc60f1a2dbdd8e6ae4fee4fdf83adbac275b9821b2ac05ca72b1d597babd51f
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
