FROM elastic_jni_build_java_includes:latest AS java_includes

FROM dockcross/linux-arm64-musl@sha256:b3b564d2696217bb41a08228d7a0683aa8ee5f62f3cc1d4e635b35d02dbd9870
COPY --from=java_includes /java_linux /java_linux

CMD $CXX -I /java_linux/include -I /java_linux/include/linux $BUILD_ARGS
