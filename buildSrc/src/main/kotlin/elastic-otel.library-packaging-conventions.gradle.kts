import java.util.stream.Collectors

plugins {
  `java-library`
  id("elastic-otel.java-conventions")
}

tasks {

  jar {
    // include licenses and notices in jar
    from("${rootDir}") {
      into("META-INF")

      include("LICENSE")
      include("NOTICE")
    }
  }

  // Generates a Java class exposing the project version
  val generateVersionProviderSource by registering() {
    val packageName = "co.elastic.otel";
    val className = kebapToCamelCase(project.name)+"Version";

    val outputDir = layout.buildDirectory.dir("generated/version-provider-source")
    outputs.dir(outputDir)
    doLast {
      val packageDir = outputDir.get().asFile.resolve(packageName.replace('.', '/'))
      packageDir.mkdirs()
      val source = """
        package $packageName;
        
        /**
         * This class is generated.
        */
        public class $className {
          public static final String VERSION = "${project.version}";
        }
      """.trimIndent()
      packageDir.resolve("$className.java").writeText(source)
    }
  }
}

sourceSets {
  main {
    java {
      srcDir(tasks.getByName("generateVersionProviderSource"));
    }
  }
}

fun kebapToCamelCase(name : String) : String {
  val words = name.split("-");
  return words.stream()
    .map { w -> Character.toUpperCase(w[0]) + w.substring(1) }
    .collect(Collectors.joining())
}
