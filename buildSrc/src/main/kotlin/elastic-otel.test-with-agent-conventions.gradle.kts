plugins {
  id("elastic-otel.java-conventions")
}

val agentForTesting: Configuration by configurations.creating

dependencies {
  agentForTesting(project(":testing:agent-for-testing"))
  testImplementation("io.opentelemetry.javaagent:opentelemetry-testing-common")
}

tasks.withType<Test>() {
  dependsOn(agentForTesting)
  useJUnitPlatform()
  jvmArgs("-javaagent:${agentForTesting.singleFile.absoluteFile}")
}
