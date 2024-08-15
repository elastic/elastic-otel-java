plugins {
  id("elastic-otel.java-conventions")
  id("java-library")
}

dependencies {
  api("io.opentelemetry:opentelemetry-sdk-testing")
  implementation("io.opentelemetry:opentelemetry-api-incubator")
  implementation("io.opentelemetry:opentelemetry-exporter-logging")
  implementation(enforcedPlatform(catalog.junitBom))
  implementation("org.junit.jupiter:junit-jupiter")

  compileOnly("io.opentelemetry:opentelemetry-sdk")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
}
