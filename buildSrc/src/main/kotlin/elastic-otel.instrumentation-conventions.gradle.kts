import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  `java-library`
  id("com.gradleup.shadow")
  id("elastic-otel.java-conventions")
  // NOTE: We can't declare a dependency on the muzzle-check and muzzle-generation here
  // Unfortunately those pull in ancient version of apache-httpclient if used as dependency in buildSrc/build.gradle
  // That ancient version would then be used in the buildEnvironment of ALL other modules, including the smoke tests
  // If there is any other plugin using apache httpclient (like jib) it will fail due to the ancient http client version
  // we workaround this problem by making instrumentation modules pull in the dependency instead of doing it globally here
}

// Other instrumentations to include for testing
val testInstrumentation: Configuration by configurations.creating {
  isCanBeConsumed = false
}
val agentForTesting: Configuration by configurations.creating {
  isCanBeConsumed = false
}

//https://github.com/gradle/gradle/issues/15383
val catalog = extensions.getByType<VersionCatalogsExtension>().named("catalog")
dependencies {
  agentForTesting(platform(catalog.findLibrary("opentelemetryInstrumentationAlphaBom").get()))
  agentForTesting("io.opentelemetry.javaagent:opentelemetry-agent-for-testing")

  compileOnly("io.opentelemetry:opentelemetry-sdk")
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")

  testImplementation("io.opentelemetry.javaagent:opentelemetry-testing-common")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")

  val agentVersion = catalog.findVersion("opentelemetryJavaagentAlpha").get()
  add("codegen", "io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:${agentVersion}")
  add("muzzleBootstrap", "io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations-support:${agentVersion}")
  add("muzzleTooling", "io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api:${agentVersion}")
  add("muzzleTooling", "io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:${agentVersion}")
}

fun relocatePackages( shadowJar : ShadowJar) {
  // rewrite dependencies calling Logger.getLogger
  shadowJar.relocate("java.util.logging.Logger", "io.opentelemetry.javaagent.bootstrap.PatchLogger")

  // prevents conflict with library instrumentation, since these classes live in the bootstrap class loader
  shadowJar.relocate("io.opentelemetry.instrumentation", "io.opentelemetry.javaagent.shaded.instrumentation") {
    // Exclude resource providers since they live in the agent class loader
    exclude("io.opentelemetry.instrumentation.resources.*")
    exclude("io.opentelemetry.instrumentation.spring.resources.*")
  }

  // relocate(OpenTelemetry API) since these classes live in the bootstrap class loader
  shadowJar.relocate("io.opentelemetry.api", "io.opentelemetry.javaagent.shaded.io.opentelemetry.api")
  shadowJar.relocate("io.opentelemetry.semconv", "io.opentelemetry.javaagent.shaded.io.opentelemetry.semconv")
  shadowJar.relocate("io.opentelemetry.context", "io.opentelemetry.javaagent.shaded.io.opentelemetry.context")
  shadowJar.relocate("io.opentelemetry.extension.incubator", "io.opentelemetry.javaagent.shaded.io.opentelemetry.extension.incubator")

  // relocate the OpenTelemetry extensions that are used by instrumentation modules
  // these extensions live in the AgentClassLoader, and are injected into the user's class loader
  // by the instrumentation modules that use them
  shadowJar.relocate("io.opentelemetry.extension.aws", "io.opentelemetry.javaagent.shaded.io.opentelemetry.extension.aws")
  shadowJar.relocate("io.opentelemetry.extension.kotlin", "io.opentelemetry.javaagent.shaded.io.opentelemetry.extension.kotlin")
}

tasks {
  shadowJar {
    configurations = listOf(project.configurations.runtimeClasspath.get(), testInstrumentation)
    mergeServiceFiles()

    archiveFileName.set("agent-testing.jar")
    relocatePackages(this)
  }
}

tasks.withType<Test>().configureEach {
  dependsOn(tasks.shadowJar, agentForTesting)

  jvmArgs(
    "-Dotel.javaagent.debug=true",
    "-javaagent:${agentForTesting.files.first().absolutePath}",
    // loads the given just jar, but in contrast to external extensions doesn't perform runtime shading
    // instead the instrumentations are expected to be correctly shaded already in the jar
    // Also the classes end up in the agent classloader instead of the extension loader
    "-Dotel.javaagent.experimental.initializer.jar=${tasks.shadowJar.get().archiveFile.get().asFile.absolutePath}",
    "-Dotel.javaagent.testing.additional-library-ignores.enabled=false",
    "-Dotel.javaagent.testing.fail-on-context-leak=true",
    "-Dotel.javaagent.testing.transform-safe-logging.enabled=true",
    "-Dotel.metrics.exporter=otlp"
  )

  // The sources are packaged into the testing jar so we need to make sure to exclude from the test
  // classpath, which automatically inherits them, to ensure our shaded versions are used.
  classpath = classpath.filter {
    return@filter !(it == file("${layout.buildDirectory.get()}/resources/main") || it == file("${layout.buildDirectory.get()}/classes/java/main"))
  }
}
