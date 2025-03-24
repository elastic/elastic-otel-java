plugins {
  application
  java

  id("com.gradleup.shadow") version "8.3.5"
}

group = "troubleshooting.example"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
}

dependencies {
  implementation(platform("io.opentelemetry:opentelemetry-bom:1.48.0"))

  implementation("io.opentelemetry:opentelemetry-api")
  implementation("io.opentelemetry:opentelemetry-sdk")
  implementation("io.opentelemetry:opentelemetry-exporter-otlp")
}

application {
  mainClass = "elastic.troubleshooting.Main"
}

tasks {
  shadowJar {
    archiveFileName.set("troubleshooting.jar")
  }
}


