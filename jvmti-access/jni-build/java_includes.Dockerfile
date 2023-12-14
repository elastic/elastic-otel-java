FROM ubuntu:jammy-20231128
RUN apt-get update && apt-get install -y curl unzip

RUN mkdir /java_linux && cd /java_linux \
    && curl -L https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.6%2B10/OpenJDK17U-jdk_x64_linux_hotspot_17.0.6_10.tar.gz --output jdk.tar.gz \
    && tar --strip-components 1 -xvf jdk.tar.gz --wildcards jdk*/include \
    && rm jdk.tar.gz

RUN mkdir /java_darwin && cd /java_darwin \
    && curl -L https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.6%2B10/OpenJDK17U-jdk_aarch64_mac_hotspot_17.0.6_10.tar.gz --output jdk.tar.gz \
    && tar --strip-components 3 -xvf jdk.tar.gz --wildcards jdk*/include \
    && rm jdk.tar.gz
