plugins {
  java
  id("com.gradleup.shadow")
  id("com.diffplug.spotless")
}

group = "baggage.extension"
version = "1.0-SNAPSHOT"

val instrumentationVersion = "2.23.0-alpha"

repositories {
  mavenCentral()
}

dependencies {

  compileOnly(platform("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom:${instrumentationVersion}"))
  compileOnly(platform("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha:${instrumentationVersion}"))

  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-incubator")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")

  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
}

tasks {
  compileJava {
    options.release.set(8)
  }
  shadowJar {
    archiveFileName.set("baggage-extension.jar")
  }
  assemble {
    dependsOn(shadowJar)
  }
}
