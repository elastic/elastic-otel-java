import java.nio.file.Paths

plugins {
  id("elastic-otel.test-with-agent-conventions")
}

dependencies {
  testImplementation(project(":testing-common"))
}


tasks.withType<Test>() {

  // We need a short temporary path, because the socket file path length is limited to only ~100 chars
  // therefore we can't use this.temporaryDir reliably

  val tmpDir=Paths.get(System.getProperty("java.io.tmpdir"))
    .resolve("proftest"+System.nanoTime())
    .toAbsolutePath().toString()

  jvmArgs(
    //"-Dotel.javaagent.debug=true",
    "-Dotel.service.name=testing",
    "-Delastic.otel.universal.profiling.integration.socket.dir=${tmpDir}"
  )
}
