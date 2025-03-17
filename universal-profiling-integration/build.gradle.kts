plugins {
    id("elastic-otel.library-packaging-conventions")
    id("elastic-otel.sign-and-publish-conventions")
    alias(libs.plugins.jmh)
}

jmh {
  fork = 1
  iterations = 5
  warmupIterations = 3
  //profilers.add("jfr")
}

description = "OpenTelemetry SDK extension to enable correlation of traces with elastic universal profiling"

dependencies {
  implementation(project(":jvmti-access"))
  api(project(":common"))
  implementation(libs.opentelemetrySemconv)
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  implementation(libs.lmax.disruptor)
  implementation(libs.hdrhistogram) //only used for the WriterReaderPhaser

  compileOnly("io.opentelemetry:opentelemetry-sdk")

  testImplementation(project(":testing-common"))
  testImplementation("io.opentelemetry:opentelemetry-sdk")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  testImplementation(libs.bundles.semconv)
}
