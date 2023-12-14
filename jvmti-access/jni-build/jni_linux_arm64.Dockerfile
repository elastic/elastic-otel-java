
FROM elastic_jni_build_java_includes:latest AS java_includes

FROM dockcross/linux-arm64
COPY --from=java_includes /java_linux /java_linux

CMD $CXX -I /java_linux/include -I /java_linux/include/linux $BUILD_ARGS
