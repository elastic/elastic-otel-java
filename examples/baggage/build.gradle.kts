plugins {
  application
  java

  id("com.gradleup.shadow") version "8.3.5"
}

group = "baggage.example"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
}

dependencies {
  implementation("co.elastic.otel:elastic-otel-runtime-attach:1.4.1")
  implementation(platform("org.slf4j:slf4j-bom:2.0.16"))
  implementation("org.slf4j:slf4j-simple")

  // otel API to access Baggage API
  implementation("io.opentelemetry:opentelemetry-api:1.51.0")

}

application {
  mainClass = "baggage.example.Main"
}

tasks {
  compileJava {
    options.release.set(21)
  }

  shadowJar {
    archiveFileName.set("baggage-example-all.jar")
  }

  assemble {
    dependsOn("shadowJar")
  }
}
