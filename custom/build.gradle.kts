plugins {
  id("elastic-otel.library-packaging-conventions")
}

dependencies {
  implementation(project(":common"))
  implementation(project(":inferred-spans"))
  implementation(project(":universal-profiling-integration"))
  compileOnly(project(":bootstrap"))
  implementation(project(":resources"))
  compileOnly("io.opentelemetry:opentelemetry-sdk")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")
  compileOnly(libs.bundles.semconv)

  implementation(libs.contribSpanStacktrace)

  annotationProcessor(libs.autoservice.processor)
  compileOnly(libs.autoservice.annotations)

  // needs to be added in order to allow access to AgentListener interface
  // this is currently required because autoconfigure is currently not exposed to the extension API.
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")

  // test dependencies
  testImplementation(project(":testing-common"))
  testImplementation("io.opentelemetry:opentelemetry-sdk")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  testImplementation("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling") {
    //The following dependency isn't actually needed, but breaks the classpath when testing with Java 8
    exclude(group = "io.opentelemetry.javaagent", module = "opentelemetry-javaagent-tooling-java9")
  }
  testImplementation(libs.bundles.semconv)

  testAnnotationProcessor(libs.autoservice.processor)
  testCompileOnly(libs.autoservice.annotations)

  testImplementation("io.opentelemetry.javaagent:opentelemetry-testing-common")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
  testImplementation(libs.assertj.core)
  testImplementation(libs.freemarker)
}
tasks.withType<Test> {
  val overrideConfig = project.properties["elastic.otel.overwrite.config.docs"]
  if (overrideConfig != null) {
    systemProperty("elastic.otel.overwrite.config.docs", overrideConfig)
  }
}
