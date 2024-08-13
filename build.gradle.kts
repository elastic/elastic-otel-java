import java.io.FileInputStream
import java.nio.file.Paths
import java.nio.file.Files
import java.nio.file.StandardCopyOption


val versionProperties = java.util.Properties()
versionProperties.load(FileInputStream(file("version.properties")))

group = "co.elastic.otel"
version = versionProperties.get("version").toString()
description = "Elastic Distribution for OpenTelemetry Java"

defaultTasks("agent:assemble")

subprojects {
  group = rootProject.group
  version = rootProject.version
}

tasks {

  register("currentVersion") {
    doLast {
      println(project.version)
    }
  }

  register("setVersion") {
    doLast {
      val versionFile = file("version.properties")
      versionFile.writeText("version=${project.property("newVersion")}\n")
    }
  }

  register("setNextVersion") {
    doLast {
      val semVer = "\\d+\\.\\d+\\.\\d+".toRegex().find(version.toString())!!.value;
      val nums = semVer.split('.');

      val versionFile = file("version.properties")
      versionFile.writeText("version=${nums[0]}.${nums[1]}.${nums[2].toInt() + 1}-SNAPSHOT\n")
    }
  }
}



//Copy githooks automatically when gradle configures the project
Files.copy(
  Paths.get(layout.projectDirectory.file(".githooks/pre-commit").toString()),
  Paths.get(layout.projectDirectory.file(".git/hooks/pre-commit").toString()),
  StandardCopyOption.REPLACE_EXISTING
)
