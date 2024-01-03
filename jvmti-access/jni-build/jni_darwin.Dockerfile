FROM crazymax/osxcross:13.1-r0-ubuntu AS osxcross
FROM elastic_jni_build_java_includes:latest AS java_includes

FROM ubuntu:jammy-20231128
RUN apt-get update && apt-get install -y curl clang lld libc6-dev
ENV PATH="/osxcross/bin:$PATH"
ENV LD_LIBRARY_PATH="/osxcross/lib:$LD_LIBRARY_PATH"

COPY --from=osxcross /osxcross /osxcross
COPY --from=java_includes /java_darwin /java_darwin

CMD o64-clang++ -I /java_darwin/include -I /java_darwin/include/darwin $BUILD_ARGS
