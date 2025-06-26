plugins {
    application
    java

    id("com.gradleup.shadow") version "8.3.5"
}

group = "baggage.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.slf4j:slf4j-bom:2.0.16"))
    implementation("com.openai:openai-java:1.2.0")
    implementation("org.slf4j:slf4j-simple")
}

application {
    mainClass = "openai.example.Chat"
}

tasks {
    compileJava {
        options.release.set(21)
    }

    shadowJar {
        archiveFileName.set("openai-example-all.jar")
    }

    assemble {
        dependsOn("shadowJar")
    }
}
