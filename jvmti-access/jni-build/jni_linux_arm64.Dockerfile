
FROM elastic_jni_build_java_includes:latest AS java_includes

FROM dockcross/linux-arm64@sha256:d1e0059c199d64c74f2e813ce71e210b55ec9c1b24fdb14520c6125d5119513f
COPY --from=java_includes /java_linux /java_linux

CMD $CXX -I /java_linux/include -I /java_linux/include/linux $BUILD_ARGS
