plugins {
    id("elastic-otel.test-with-agent-conventions")
}

tasks.test {
  jvmArgs(
    //"-Dotel.javaagent.debug=true"
  )
}
