plugins {
  id("elastic-otel.instrumentation-conventions")
}

dependencies {
  compileOnly("com.openai:openai-java:0.11.2")

  testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.18.2")
  testImplementation("com.openai:openai-java:0.8.1")
  testImplementation("org.slf4j:slf4j-simple:2.0.16")
}


tasks {

  /*
  testing {
    suites {
      withType<JvmTestSuite> {
        dependencies {
          implementation("com.openai:openai-java:0.8.1")
          implementation("org.slf4j:slf4j-simple:2.0.16")
        }
      }

    }
  }
  */
}
