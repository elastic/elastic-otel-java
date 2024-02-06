plugins {
  id("java")
}

val agentForTesting: Configuration by configurations.creating

dependencies {
  agentForTesting(project(":testing:agent-for-testing"))
  testImplementation("io.opentelemetry.javaagent:opentelemetry-testing-common")
}

tasks.test {
  dependsOn(":testing:agent-for-testing:assemble")
  useJUnitPlatform()
  jvmArgs("-javaagent:${agentForTesting.singleFile.absoluteFile}")
}
