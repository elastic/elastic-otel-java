plugins {
  java
  id("com.github.johnrengelman.shadow")
  alias(catalog.plugins.taskinfo)
}

dependencies {

  implementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")

  // those are already included in the upstream agent
  compileOnly(catalog.gcpContribResources)
  compileOnly(catalog.awsContribResources)

  // auto-service
  annotationProcessor(libs.autoservice.processor)
  compileOnly(libs.autoservice.annotations)

  // not included in the upstream agent
  implementation(catalog.contribResources)

  testImplementation(libs.assertj.core)
}
