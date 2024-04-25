FROM elastic_jni_build_java_includes:latest AS java_includes

FROM --platform=linux/arm64 alpine:latest
COPY --from=java_includes /java_linux /java_linux

RUN apk add build-base

CMD g++ -I /java_linux/include -I /java_linux/include/linux $BUILD_ARGS
