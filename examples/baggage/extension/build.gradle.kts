plugins {
  java
  id("com.gradleup.shadow")
  id("com.diffplug.spotless")
  id("io.opentelemetry.instrumentation.muzzle-generation")
  id("io.opentelemetry.instrumentation.muzzle-check")
}

group = "baggage.extension"
version = "1.0-SNAPSHOT"

val instrumentationVersion = "2.22.0-alpha"

repositories {
  mavenCentral()
}

dependencies {

  // dependencies for muzzle and codegen configurations (required by muzzle gradle plugin)
  muzzleBootstrap("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api:${instrumentationVersion}")
  muzzleBootstrap("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-incubator:${instrumentationVersion}")
  muzzleBootstrap("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations-support:${instrumentationVersion}")
  muzzleTooling("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api:${instrumentationVersion}")
  muzzleTooling("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:${instrumentationVersion}")
  codegen("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:${instrumentationVersion}")

  // "regular" extension dependencies

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
