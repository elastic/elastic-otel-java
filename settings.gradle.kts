pluginManagement {
  repositories {
    gradlePluginPortal()
    maven {
      name = "sonatype"
      url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }
  }
  plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
  }
}

rootProject.name = "elastic-otel-java"

include("agent")
include("bootstrap")
include("custom")
include("instrumentation")
include("smoke-tests")
include("testing:agent-for-testing")

//include("test-app")
