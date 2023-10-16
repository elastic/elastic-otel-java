plugins {
  alias(gradlePlugins.plugins.shadow)
  alias(gradlePlugins.plugins.jib)
  alias(gradlePlugins.plugins.taskinfo)
}

dependencies {

  // Using a spring boot app is simpler for a general-purpose test app
  // - spring-boot part is already tested in the upstream agent, thus we don't have to test it again
  // - http endpoint, which is easy to call remotely
  // - implement a server, which is not a single invocation only like a CLI app
  // - multiple endpoints are possible, which allows multiple test scenarios
  val springBootVersion = "2.7.15";
  testImplementation("org.springframework.boot:spring-boot-starter-test:${springBootVersion}")
  implementation("org.springframework.boot:spring-boot-starter-web:${springBootVersion}")


  implementation("io.opentelemetry:opentelemetry-api")
  implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations")

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
  container.mainClass = "co.elastic.otel.test.AppMain"
}

tasks {

  // can we set the 'jib' task output to be the list of files that are used as 'input' of the jib task ?

  // javadoc not required
  javadoc {
    isEnabled = false
  }

  // build docker image with 'assemble'
  assemble {
    dependsOn(jibDockerBuild)
  }

}
