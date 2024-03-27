plugins {
    id("elastic-otel.library-packaging-conventions")
    alias(libs.plugins.jmh)
}

jmh {
  fork = 1
  iterations = 5
  warmupIterations = 3
  //profilers.add("jfr")
}

dependencies {
  implementation(project(":jvmti-access"))
  implementation(project(":common"))
  implementation("io.opentelemetry.semconv:opentelemetry-semconv")
  implementation(libs.disruptor)
  implementation(libs.hdrhistogram) //only used for the WriterReaderPhaser

  compileOnly("io.opentelemetry:opentelemetry-sdk")
  compileOnly(libs.findbugs.jsr305)

  testCompileOnly(libs.findbugs.jsr305)
  testImplementation(project(":testing-common"))
  testImplementation("io.opentelemetry:opentelemetry-sdk")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
  testImplementation(libs.assertj.core)
  testImplementation(libs.awaitility)
}
