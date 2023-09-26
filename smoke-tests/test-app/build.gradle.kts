
plugins {
    java

    id("org.springframework.boot") version "2.7.15" // TODO : store spring boot version in a single location
    id("io.spring.dependency-management") version "1.1.3"
}
// TODO : package as a single jar

dependencies {

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    implementation("org.springframework.boot:spring-boot-starter-web")

}

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks {
    javadoc {
        isEnabled = false
    }

    // TODO : using a spring boot app is simpler for a general-purpose app
    // - spring-boot part is already tested in the upstream agent, thus we don't have to test it again
    // - http endpoint, which is easy to call remotely
    // - implement a server, which is not a single invocation only like a CLI app
    // - multiple endpoints are possible, which allows multiple test scenarios
}