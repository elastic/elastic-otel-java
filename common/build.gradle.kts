plugins {
  id("java-library")
  id("elastic-otel.library-packaging-conventions")
  id("elastic-otel.sign-and-publish-conventions")
}

description = "Elastic common utilities for OpenTelemetry Java"

dependencies {
    api("com.blogspot.mydailyjava:weak-lock-free:0.18")
    compileOnly("io.opentelemetry:opentelemetry-sdk")
    compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
    implementation(libs.bundles.semconv)

    testImplementation("io.opentelemetry:opentelemetry-sdk")
    testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
    testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
    testImplementation(project(":testing-common"))
    testImplementation("io.opentelemetry:opentelemetry-exporter-otlp")
    testImplementation("io.opentelemetry:opentelemetry-exporter-logging")
    testImplementation("io.opentelemetry:opentelemetry-api-incubator")
}
