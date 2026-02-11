plugins {
  id("com.gradleup.shadow")
  id("elastic-otel.java-conventions")
  alias(catalog.plugins.jib)
  alias(catalog.plugins.taskinfo)
}

val mainClass = "co.elastic.otel.test.AppMain"

dependencies {

  // Using a spring boot app is simpler for a general-purpose test app
  // - spring-boot part is already tested in the upstream agent, thus we don't have to test it again
  // - http endpoint, which is easy to call remotely
  // - implement a server, which is not a single invocation only like a CLI app
  // - multiple endpoints are possible, which allows multiple test scenarios
  val springBootVersion = "2.7.18";
  testImplementation("org.springframework.boot:spring-boot-starter-test:${springBootVersion}")
  implementation("org.springframework.boot:spring-boot-starter-web:${springBootVersion}")

  implementation("io.opentelemetry:opentelemetry-api")
  implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations")

  implementation(project(":runtime-attach"))

  implementation("org.springframework.boot:spring-boot-starter-artemis:${springBootVersion}")
  // using a rather old version to keep java 8 compatibility
  implementation("org.apache.activemq:artemis-jms-server:2.51.0")
}

java {
  // java 8 since using spring boot 2.x
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

// using the 'latest' label by default
// TODO: use release version tag when doing a release
val tag = findProperty("tag") ?: "latest"

jib {
  from.image = "gcr.io/distroless/java17-debian11:debug"
  to.image = "docker.elastic.co/open-telemetry/elastic-otel-java/smoke-test/test-app:$tag"
  container.ports = listOf("8080")
  container.mainClass = mainClass
}

tasks {

  // can we set the 'jib' task output to be the list of files that are used as 'input' of the jib task ?

  // javadoc not required
  javadoc {
    isEnabled = false
  }

  jar {
    manifest {
      attributes("Main-Class" to mainClass)
    }
  }

  // build docker image with 'assemble'
  assemble {
    dependsOn(jibDockerBuild)
  }

}
