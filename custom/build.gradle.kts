plugins {
  id("elastic-otel.library-packaging-conventions")
}

val instrumentations = listOf<String>(
  ":instrumentation:openai-client-instrumentation:instrumentation-0.13.0",
  ":instrumentation:openai-client-instrumentation:instrumentation-latest"
)

dependencies {
  implementation(project(":common"))
  implementation(project(":inferred-spans"))
  implementation(project(":universal-profiling-integration"))
  implementation(project(":resources"))
  instrumentations.forEach {
    implementation(project(it))
  }

  compileOnly("io.opentelemetry:opentelemetry-sdk")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")
  compileOnly(libs.bundles.semconv)

  implementation(libs.contribSpanStacktrace) {
    // exclude transitive dependency as it's provided through agent packaging
    exclude(group = "io.opentelemetry", module = "opentelemetry-sdk")
  }
  testImplementation(libs.contribSpanStacktrace)

  // needs to be added in order to allow access to AgentListener interface
  // this is currently required because autoconfigure is currently not exposed to the extension API.
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")

  // test dependencies
  testImplementation(project(":testing-common"))
  testImplementation("io.opentelemetry:opentelemetry-sdk")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  testImplementation("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling") {
    //The following dependency isn't actually needed, but breaks the classpath when testing with Java 8
    exclude(group = "io.opentelemetry.javaagent", module = "opentelemetry-javaagent-tooling-java9")
  }
  testImplementation(libs.bundles.semconv)

  testImplementation("io.opentelemetry.javaagent:opentelemetry-testing-common")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
  testImplementation(libs.freemarker)
}

tasks {
  instrumentations.forEach {
    // TODO: instrumentation dependencies must be declared here explicitly atm, otherwise gradle complains
    // about it being missing we need to figure out a way of including it in the "compileJava" task
    // to not have to do this
    javadoc {
      dependsOn("$it:byteBuddyJava")
    }
    compileTestJava {
      dependsOn("$it:byteBuddyJava")
    }
  }
}

tasks.withType<Test> {
  val overrideConfig = project.properties["elastic.otel.overwrite.config.docs"]
  if (overrideConfig != null) {
    systemProperty("elastic.otel.overwrite.config.docs", overrideConfig)
  }
}
