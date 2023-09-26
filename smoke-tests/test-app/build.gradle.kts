
// Using a spring boot app is simpler for a general-purpose test app
// - spring-boot part is already tested in the upstream agent, thus we don't have to test it again
// - http endpoint, which is easy to call remotely
// - implement a server, which is not a single invocation only like a CLI app
// - multiple endpoints are possible, which allows multiple test scenarios

plugins {
    id("org.springframework.boot") version "2.7.15"
    id("io.spring.dependency-management") version "1.1.3"
}

dependencies {

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation("io.opentelemetry:opentelemetry-api")
    implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

tasks {

    // javadoc not required
    javadoc {
        isEnabled = false
    }
}