import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    alias(gradlePlugins.plugins.shadow)
    alias(gradlePlugins.plugins.jib)
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

// TODO not sure about the "time-based" tag strategy as it's quite hard to refer to it in the tests
// maybe we could find a way to inject the commit ID of the last change, hence ensuring that it's the one we expect.
// using '<short-commit-hash>' or '<short-commit-hash>-dirty'
val tag = findProperty("tag")
    ?: DateTimeFormatter.ofPattern("yyyyMMdd.HHmmSS").format(LocalDateTime.now())

jib {
    from.image = "gcr.io/distroless/java17-debian11:debug"
    to.image = "docker.elastic.co/open-telemetry/elastic-otel-java/smoke-test/test-app:$tag"
    container.ports = listOf("8080")
    container.mainClass = "co.elastic.otel.test.AppMain"
}

tasks {

    // javadoc not required
    javadoc {
        isEnabled = false
    }
}