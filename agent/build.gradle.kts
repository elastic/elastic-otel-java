import com.github.jk1.license.filter.LicenseBundleNormalizer
import com.github.jk1.license.render.InventoryMarkdownReportRenderer
import java.nio.file.Files
import java.util.*
import kotlin.collections.ArrayList

plugins {
  id("maven-publish")
  id("signing")
  id("elastic-otel.agent-packaging-conventions")
  alias(catalog.plugins.taskinfo)
  alias(catalog.plugins.licenseReport)
}

description = rootProject.description + " agent"

base.archivesName.set("elastic-otel-javaagent")

dependencies {
  // required to access OpenTelemetryAgent
  compileOnly(catalog.opentelemetryJavaagentBootstrap)

  upstreamAgent(catalog.opentelemetryJavaagent)
}


tasks {

  val cleanLicenses by registering(Delete::class) {
    Files.list(rootDir.toPath().resolve("licenses"))
      .filter { f -> Files.isDirectory(f) || f.fileName.startsWith("NOTICE_") }
      .forEach { f -> delete(f) }
  }
  generateLicenseReport {
    dependsOn(cleanLicenses)
  }

  // We override the agent entrypoints defined in elastic-otel.agent-packaging-convention
  shadowJar {

    //TODO: The agent-for-testing should also use our custom entrypoint
    manifest {
      attributes["Main-Class"] = "co.elastic.otel.agent.ElasticAgent"
      attributes["Agent-Class"] = "co.elastic.otel.agent.ElasticAgent"
      attributes["Premain-Class"] = "co.elastic.otel.agent.ElasticAgent"
    }
  }

  assemble {
    dependsOn(javadocJar, sourcesJar)
  }

  licenseReport {
    outputDir = "${rootProject.rootDir}/licenses"
    renderers = arrayOf(InventoryMarkdownReportRenderer("more-licences.md"))
    excludeBoms = true
    excludes = arrayOf(
      "io.opentelemetry:opentelemetry-bom-alpha",
      "io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha"
    )
    filters = arrayOf(LicenseBundleNormalizer("${rootProject.rootDir}/buildscripts/license-normalizer-bundle.json", true))
    projects = arrayOf(rootProject, rootProject.project("agent"), rootProject.project("bootstrap"),
      rootProject.project("common"), rootProject.project("custom"),
      rootProject.project("inferred-spans"), rootProject.project("instrumentation"),
      rootProject.project("resources"), project)

    configurations = arrayOf("runtimeClasspath", "compileClasspath")
  }

  register("update-licenses-and-notice") {
    dependsOn(generateLicenseReport)
    doLast {

      var lines = ArrayList<String>(
        listOf(
          "Elastic OpenTelemetry Java Distribution",
          String.format(
            "Copyright 2023-%d Elasticsearch B.V.",
            Calendar.getInstance().get(Calendar.YEAR)
          ),
          "",
          "This project is licensed under the Apache License, Version 2.0 - https://www.apache.org/licenses/LICENSE-2.0",
          "A copy of the Apache License, Version 2.0 is provided in the 'LICENSE' file.",
          "",
          "This project depends on ASM which is licensed under the 3-Clause BSD License - https://opensource.org/licenses/BSD-3-Clause",
          "A copy of the ASM license (3-Clause BSD License) is provided in the 'licenses/LICENSE_bsd-3-clause' file.",
          "",
          "This project depends on okhttp, which contains 'publicsuffixes.gz' that is licensed under the Mozilla Public License, v. 2.0 - https://mozilla.org/MPL/2.0/",
          "A copy of the publicsuffixes.gz license (Mozilla Public License, v. 2.0) is provided in the 'licenses/LICENSE_mpl-2' file.",
          "",
          "Details on the individual dependencies notices and licenses can be found in the 'licenses' folder.",
          ""
        )
      )

      Files.write(rootDir.toPath().resolve("NOTICE"), lines)
    }
  }

}

publishing {
  publications {
    register("agentJar", MavenPublication::class) {
      artifactId = "elastic-otel-javaagent"

      artifact(tasks.shadowJar.get())
      artifact(tasks.sourcesJar.get())
      artifact(tasks.javadocJar.get())

      pom {
        name.set(project.description)
        description.set(project.description)
        url.set("https://github.com/elastic/elastic-otel-java")
        licenses {
          license {
            name.set("The Apache License, Version 2.0")
            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
          }
        }
        developers {
          developer {
            name.set("Elastic Inc.")
            url.set("https://www.elastic.co")
          }
        }
        scm {
          connection.set("scm:git:git@github.com:elastic/elastic-otel-java.git")
          developerConnection.set("scm:git:git@github.com:elastic/elastic-otel-java.git")
          url.set("https://github.com/elastic/elastic-otel-java")
        }
      }
    }
  }
}


signing {
  setRequired({
    // only sign in CI
    System.getenv("CI") == "true"
  })
  // use in-memory ascii-armored key in environment variables
  useInMemoryPgpKeys(System.getenv("KEY_ID_SECRET"), System.getenv("SECRING_ASC"), System.getenv("KEYPASS_SECRET"))
  sign(publishing.publications["agentJar"])
}
