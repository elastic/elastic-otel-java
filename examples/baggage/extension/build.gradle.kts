plugins {
  java
}

group = "baggage.extension"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
}

dependencies {
  // TODO: make sure this dependency gets updated by dependabot/renovate
  implementation("io.opentelemetry:opentelemetry-api:1.51.0")

}

tasks {
  compileJava {
    options.release.set(21)
  }
}
