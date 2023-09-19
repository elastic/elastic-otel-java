repositories {
    maven {
        url = uri("https://plugins.gradle.org/m2/")
    }

    // maven central snapshots for upstream opentelemetry
    maven {
        name = "sonatype"
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }
}

dependencies {
    implementation("com.diffplug.spotless:spotless-plugin-gradle:6.20.0")
    implementation("com.github.johnrengelman:shadow:8.1.1")
    // provides muzzle gradle plugin
    implementation("io.opentelemetry.instrumentation:gradle-plugins:1.30.0-alpha-SNAPSHOT")
}