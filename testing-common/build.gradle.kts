plugins {
  id("java-library")
}

dependencies {
  annotationProcessor(libs.autoservice.processor)
  compileOnly(libs.autoservice.annotations)

  implementation(libs.assertj.core)
  implementation("io.opentelemetry:opentelemetry-sdk-testing")
  implementation("io.opentelemetry:opentelemetry-exporter-logging")
  implementation(enforcedPlatform("org.junit:junit-bom:" + catalog.versions.junit.get()))
  implementation("org.junit.jupiter:junit-jupiter")

  compileOnly(libs.findbugs.jsr305)
  compileOnly("io.opentelemetry:opentelemetry-sdk")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
}
