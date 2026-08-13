plugins {
  id("java")
  id("com.gradleup.shadow")
}

repositories {
  mavenCentral()
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(8))
  }
}

dependencies {
  implementation(platform("io.opentelemetry:opentelemetry-bom:1.64.0"))

  implementation("io.opentelemetry.semconv:opentelemetry-semconv:1.43.0")

  implementation("io.opentelemetry:opentelemetry-api")
}

tasks.assemble {
  dependsOn(tasks.shadowJar)
}

tasks.jar {
  manifest {
    attributes("Main-Class" to "Main")
  }
}
