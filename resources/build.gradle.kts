plugins {
  java
  alias(catalog.plugins.taskinfo)
}

dependencies {

  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")

  // auto-service
  annotationProcessor(libs.autoservice.processor)
  compileOnly(libs.autoservice.annotations)

  // not included in the upstream agent
  implementation(catalog.contribResources)

  testImplementation(libs.assertj.core)
}
