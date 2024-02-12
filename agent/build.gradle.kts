import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  id("maven-publish")
  id("signing")
  id("elastic-otel.agent-packaging-conventions")
  alias(catalog.plugins.taskinfo)
}

description = rootProject.description + " agent"

base.archivesName.set("elastic-otel-javaagent")

dependencies {
  // required to access OpenTelemetryAgent
  compileOnly(catalog.opentelemetryJavaagentBootstrap)

  upstreamAgent(catalog.opentelemetryJavaagent)
}

tasks {

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
