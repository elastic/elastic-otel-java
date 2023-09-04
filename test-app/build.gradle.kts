plugins {
    java
    id("com.github.johnrengelman.shadow").version("8.1.1")
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // app dependencies
    implementation("com.google.guava:guava:31.1-jre")
    implementation("io.opentelemetry:opentelemetry-api:1.29.0")
    implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:1.29.0")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}


val main = "co.elastic.apm.otel.app.App"

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = main
    }
}
