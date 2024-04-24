import gradle.kotlin.dsl.accessors._2a2fda20f7c0d5ad930aaa9c8e47b6e1.jar

plugins {
  `maven-publish`
  publishing
  signing
}

interface PublishingConventionsPluginExtension {
  /**
   * By default the convention will publish the artifacts and pom as libraries.
   * To override the behaviour provide the tasks producing the artifacts as this property.
   * This should only be required when publishing fat-jars with custom packaging.
   */
  val artifactTasks: ListProperty<Task>
}

val publishingConventions = project.extensions.create<PublishingConventionsPluginExtension>("publishingConventions")
publishingConventions.artifactTasks.convention(listOf())


afterEvaluate {

  if (project.description == null || project.description!!.isBlank()) {
    throw GradleException("Project description must be set to publish the project to maven central!")
  }

  tasks {
    // Used in CI to detect the artifacts which will be published
    // Run "./gradlew printPublishedCodeArtifacts -q" to get only the output from this task without the gradle noise
    val printPublishedCodeArtifacts by registering(Task::class) {
      doLast {
        var artifactTasks = publishingConventions.artifactTasks.get()
        if (artifactTasks.isEmpty()) {
          artifactTasks = listOf(tasks.jar.get());
        }
        for (task in artifactTasks) {
          if (task is Jar) {
            // ignore javadoc, sources, testing, etc jars
            if (task.archiveClassifier.get().isEmpty()) {
              println(task.archiveFile.get().asFile.relativeTo(rootProject.rootDir))
            }
          }
        }
      }
    }
  }

  publishing {

    repositories {
      // dry-run repository will be wiped on 'clean' task
      maven {
        name = "dryRun"
        url = rootProject.layout.buildDirectory.dir("dry-run-maven-repo").get().asFile.toURI()
      }
    }

    publications {
      register("maven", MavenPublication::class) {
        artifactId = base.archivesName.get()

        val artifactTasks = publishingConventions.artifactTasks.get()
        if (artifactTasks.isEmpty()) {
          //publish as library with dependencies
          from(components["java"])
          versionMapping {
            usage("java-api") {
              fromResolutionOf("runtimeClasspath")
            }
            usage("java-runtime") {
              fromResolutionResult()
            }
          }
        } else {
          //publish just the artifacts (fat-jars).
          for (task in artifactTasks) {
            artifact(task)
          }
        }

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
    sign(publishing.publications["maven"])
  }

}
