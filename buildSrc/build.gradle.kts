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
    implementation("com.diffplug.spotless:spotless-plugin-gradle:6.21.0")
    // TODO : for now we have to disable it because it transitively imports an older apache httpclient
    // that makes the transitive one from jib fail see https://github.com/elastic/elastic-otel-java/issues/9 for details
    // implementation("io.opentelemetry.instrumentation:gradle-plugins:1.30.0-alpha-SNAPSHOT")
}

