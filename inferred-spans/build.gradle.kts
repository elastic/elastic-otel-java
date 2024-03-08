plugins {
  `java-library`
  id("signing")
  id("elastic-otel.library-packaging-conventions")
}

description = rootProject.description + " inferred-spans"

dependencies {
  annotationProcessor(libs.autoservice.processor)
  compileOnly(libs.autoservice.annotations)
  compileOnly("io.opentelemetry:opentelemetry-sdk")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  compileOnly(libs.findbugs.jsr305)
  implementation("com.lmax:disruptor:3.4.4")
  implementation("org.jctools:jctools-core:4.0.1")
  implementation(project(":common"))

  testAnnotationProcessor(libs.autoservice.processor)
  testCompileOnly(libs.autoservice.annotations)
  testCompileOnly(libs.findbugs.jsr305)
  testImplementation(project(":testing-common"))
  testImplementation("io.opentelemetry:opentelemetry-sdk")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  testImplementation(libs.awaitility)
  testImplementation("org.kohsuke:github-api:1.133")
  testImplementation("org.apache.commons:commons-compress:1.21")
  testImplementation("tools.profiler:async-profiler:1.8.3")
}

tasks.compileJava {
  options.encoding = "UTF-8"
}

tasks.javadoc {
  options.encoding = "UTF-8"
}

tasks.processResources {
  doLast {
    val resourcesDir = sourceSets.main.get().output.resourcesDir
    val packageDir = resourcesDir!!.resolve("co/elastic/otel/profiler");
    packageDir.mkdirs();
    packageDir.resolve("inferred-spans-version.txt").writeText(project.version.toString())
  }
}

tasks.withType<Test>().all {
  jvmArgs("-Djava.util.logging.config.file="+sourceSets.test.get().output.resourcesDir+"/logging.properties")
}

publishing {
  publications {
    create<MavenPublication>("maven") {

      from(components["java"])

      versionMapping {
        usage("java-api") {
          fromResolutionOf("runtimeClasspath")
        }
        usage("java-runtime") {
          fromResolutionResult()
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
