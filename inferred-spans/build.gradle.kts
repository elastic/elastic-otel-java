plugins {
  id("elastic-otel.library-packaging-conventions")
  id("elastic-otel.sign-and-publish-conventions")
}

description = "Elastic Inferred Spans extension for OpenTelemetry Java"

dependencies {
  compileOnly("io.opentelemetry:opentelemetry-sdk")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  implementation("io.opentelemetry.contrib:opentelemetry-inferred-spans")
  implementation(project(":common"))

  testImplementation(project(":testing-common"))
  testImplementation("io.opentelemetry:opentelemetry-sdk")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  testImplementation(libs.bundles.semconv)
}

tasks.compileJava {
  options.encoding = "UTF-8"
}

tasks.javadoc {
  options.encoding = "UTF-8"
}

tasks.withType<Test>().all {
  jvmArgs("-Djava.util.logging.config.file="+sourceSets.test.get().output.resourcesDir+"/logging.properties")
}
