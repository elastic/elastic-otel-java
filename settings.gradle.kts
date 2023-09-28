pluginManagement {
  repositories {
    gradlePluginPortal()
    maven {
      name = "sonatype"
      url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }
  }
  plugins {
    // define versions but do not apply the plugins unless explicitly added to sub-projects.
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false
    id("com.google.cloud.tools.jib") version "3.4.0" apply false
    id("com.diffplug.spotless") version "6.21.0" apply false
  }
}

rootProject.name = "elastic-otel-java"

include("agent")
include("bootstrap")
include("custom")
include("instrumentation")
include("smoke-tests")
include("smoke-tests:test-app")
include("testing:agent-for-testing")