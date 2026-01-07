rootProject.name = "baggage-example"

include(":extension")

pluginManagement {
  plugins {
    id("com.gradleup.shadow").version("9.2.2")
    id("com.diffplug.spotless").version("8.1.0")
    id("io.opentelemetry.instrumentation.muzzle-generation").version("2.22.0-alpha")
    id("io.opentelemetry.instrumentation.muzzle-check").version("2.22.0-alpha")
  }

}
