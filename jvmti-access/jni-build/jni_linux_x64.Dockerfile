
FROM elastic_jni_build_java_includes:latest AS java_includes

FROM dockcross/linux-x64@sha256:89a2c6061215d923a940902fbb2c3c42fdd8a4819d2bd3d7176602f34335f075
COPY --from=java_includes /java_linux /java_linux

CMD $CXX -I /java_linux/include -I /java_linux/include/linux $BUILD_ARGS
