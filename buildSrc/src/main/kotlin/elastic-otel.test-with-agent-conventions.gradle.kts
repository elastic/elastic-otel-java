plugins {
  id("elastic-otel.java-conventions")
}

val agentDistroForTesting: Configuration by configurations.creating
val upstreamAgentForTesting: Configuration by configurations.creating
val agentExtension: Configuration by configurations.creating


//https://github.com/gradle/gradle/issues/15383
val catalog = extensions.getByType<VersionCatalogsExtension>().named("catalog")

dependencies {
  agentDistroForTesting(project(":testing:agent-for-testing"))
  testImplementation("io.opentelemetry.javaagent:opentelemetry-testing-common")

  catalog.findLibrary("opentelemetryInstrumentationAlphaBom").ifPresent {
    upstreamAgentForTesting(platform(it))
  }
  upstreamAgentForTesting("io.opentelemetry.javaagent:opentelemetry-agent-for-testing")
  agentExtension(project(":agentextension"))
}

tasks {

  val testWithVanillaAgentAndExtension by registering( Test::class) {
    dependsOn(upstreamAgentForTesting, agentExtension)
    group = "verification"
    useJUnitPlatform()
    jvmArgs("-javaagent:${upstreamAgentForTesting.singleFile.absoluteFile}")
    jvmArgs("-Dotel.javaagent.extensions=${agentExtension.singleFile.absoluteFile}")
  }
  test {
    dependsOn(testWithVanillaAgentAndExtension)
  }

  test {
    dependsOn(agentDistroForTesting)
    useJUnitPlatform()
    jvmArgs("-javaagent:${agentDistroForTesting.singleFile.absoluteFile}")
  }
}
