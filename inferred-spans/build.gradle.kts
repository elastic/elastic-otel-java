plugins {
  id("elastic-otel.library-packaging-conventions")
  id("elastic-otel.sign-and-publish-conventions")
}

description = "Elastic Inferred Spans extension for OpenTelemetry Java"

dependencies {
  annotationProcessor(libs.autoservice.processor)
  compileOnly(libs.autoservice.annotations)
  // TODO: remove explicit version of dependency and have it managed by the BOM
  // once contrib 1.37 is used by the upstream agent
  implementation("io.opentelemetry.contrib:opentelemetry-inferred-spans:1.37.0-alpha")
  compileOnly("io.opentelemetry:opentelemetry-sdk")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  implementation(project(":common"))

  testAnnotationProcessor(libs.autoservice.processor)
  testCompileOnly(libs.autoservice.annotations)
  testCompileOnly(libs.findbugs.jsr305)
  testImplementation(project(":testing-common"))
  testImplementation("io.opentelemetry:opentelemetry-sdk")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  testImplementation(libs.awaitility)
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
