plugins {
  id("java-library")
}

dependencies {
  annotationProcessor(libs.autoservice.processor)
  compileOnly(libs.autoservice.annotations)

  api(libs.assertj.core)
  api("io.opentelemetry:opentelemetry-sdk-testing")
  implementation("io.opentelemetry:opentelemetry-api-events")
  implementation("io.opentelemetry:opentelemetry-exporter-logging")
  implementation(enforcedPlatform(catalog.junitBom))
  implementation("org.junit.jupiter:junit-jupiter")

  compileOnly(libs.findbugs.jsr305)
  compileOnly("io.opentelemetry:opentelemetry-sdk")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
}
