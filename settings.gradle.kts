pluginManagement {
  repositories {
    gradlePluginPortal()
    maven {
      name = "sonatype"
      url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }
  }
}

rootProject.name = "elastic-otel-java"

include("bootstrap")
include("custom")


//include("extension")

//include("test-app")