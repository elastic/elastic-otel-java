plugins {
  id("java-library")
  id("elastic-otel.library-packaging-conventions")
  id("elastic-otel.sign-and-publish-conventions")
}

description = "Elastic common utilities for OpenTelemetry Java"

dependencies {
    annotationProcessor(libs.autoservice.processor)
    api("com.blogspot.mydailyjava:weak-lock-free:0.18")
    compileOnly(libs.autoservice.annotations)
    compileOnly("io.opentelemetry:opentelemetry-sdk")
    compileOnly(libs.findbugs.jsr305)
    compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
    implementation(libs.bundles.semconv)

    compileOnly(libs.contribSpanStacktrace)
    testImplementation(libs.contribSpanStacktrace)

    testAnnotationProcessor(libs.autoservice.processor)
    testCompileOnly(libs.autoservice.annotations)
    testCompileOnly(libs.findbugs.jsr305)
    testImplementation("io.opentelemetry:opentelemetry-sdk")
    testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
    testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
    testImplementation(project(":testing-common"))
    testImplementation("io.opentelemetry:opentelemetry-exporter-otlp")
    testImplementation("io.opentelemetry:opentelemetry-exporter-logging")
    testImplementation("io.opentelemetry:opentelemetry-api-incubator")
    testImplementation(libs.assertj.core)
}
