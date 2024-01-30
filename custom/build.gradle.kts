plugins {
  java
}

dependencies {
  implementation(project(":common"))
  compileOnly(project(":bootstrap"))
  compileOnly(project(":resources"))
  compileOnly("io.opentelemetry:opentelemetry-sdk")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")
  compileOnly(catalog.opentelemetrySemconv)

  annotationProcessor(libs.autoservice.processor)
  compileOnly(libs.autoservice.annotations)

  // needs to be added in order to allow access to AgentListener interface
  // this is currently required because autoconfigure is currently not exposed to the extension API.
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")

  // test dependencies
  testImplementation("io.opentelemetry:opentelemetry-sdk")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  testImplementation("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")
  testImplementation(catalog.opentelemetrySemconv)

  testAnnotationProcessor(libs.autoservice.processor)
  testCompileOnly(libs.autoservice.annotations)

  testImplementation("io.opentelemetry.javaagent:opentelemetry-testing-common")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
  testImplementation(catalog.assertj.core)

}
