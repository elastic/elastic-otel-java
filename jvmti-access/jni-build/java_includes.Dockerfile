FROM ubuntu:jammy-20231128
RUN apt-get update && apt-get install -y curl unzip

RUN mkdir /java_linux && cd /java_linux \
    && curl -L https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.1%2B12/OpenJDK21U-jdk_x64_linux_hotspot_21.0.1_12.tar.gz --output jdk.tar.gz \
    && tar --strip-components 1 -xvf jdk.tar.gz --wildcards jdk*/include \
    && rm jdk.tar.gz

RUN mkdir /java_darwin && cd /java_darwin \
    && curl -L https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.1%2B12/OpenJDK21U-jdk_x64_mac_hotspot_21.0.1_12.tar.gz --output jdk.tar.gz \
    && tar --strip-components 3 -xvf jdk.tar.gz --wildcards jdk*/include \
    && rm jdk.tar.gz
