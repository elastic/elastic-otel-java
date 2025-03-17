FROM elastic_jni_build_java_includes:latest AS java_includes

# This image is based on the corresponding linux-musl-arm64 image definition:
# https://github.com/dockcross/dockcross/blob/master/linux-arm64-musl/Dockerfile.in
FROM dockcross/base@sha256:f319badff9234699e1f83b7b032deada5c69b54fc526cd848edac4b377547356

ENV XCC_PREFIX /usr/xcc
ENV CROSS_TRIPLE x86_64-linux-musl
ENV CROSS_ROOT ${XCC_PREFIX}/${CROSS_TRIPLE}-cross

RUN mkdir -p ${XCC_PREFIX}
RUN curl --max-time 180 --retry 5 -LO http://musl.cc/${CROSS_TRIPLE}-cross.tgz

# Verify that the downloaded file has not been altered via sha256 checksum
RUN test "$(sha256sum -b ${CROSS_TRIPLE}-cross.tgz)" = "c5d410d9f82a4f24c549fe5d24f988f85b2679b452413a9f7e5f7b956f2fe7ea *${CROSS_TRIPLE}-cross.tgz"

RUN tar -C ${XCC_PREFIX} -xvf ${CROSS_TRIPLE}-cross.tgz

ENV CXX=${CROSS_ROOT}/bin/${CROSS_TRIPLE}-g++

# Linux kernel cross compilation variables
ENV PATH ${PATH}:${CROSS_ROOT}/bin

COPY --from=java_includes /java_linux /java_linux

CMD $CXX -I /java_linux/include -I /java_linux/include/linux $BUILD_ARGS
