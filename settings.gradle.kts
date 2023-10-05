pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            name = "sonatype"
            url = uri("https://oss.sonatype.org/content/repositories/snapshots")
        }
    }
}

rootProject.name = "elastic-otel-java"

include("agent")
include("bootstrap")
include("custom")
include("instrumentation")
include("smoke-tests")
include("smoke-tests:test-app")
include("testing:agent-for-testing")

dependencyResolutionManagement {
    versionCatalogs {
        create("gradlePlugins") {
            version("shadow", "8.1.1")
            version("jib", "3.4.0")
            version("spotless", "6.21.0")

            plugin("shadow", "com.github.johnrengelman.shadow").versionRef("shadow")
            plugin("jib", "com.google.cloud.tools.jib").versionRef("jib")
            plugin("spotless", "com.diffplug.spotless").versionRef("spotless")
            plugin("taskinfo", "org.barfuin.gradle.taskinfo").version("2.1.0")
        }

        create("libraries") {
            version("junit", "5.10.0")

            version("opentelemetrySdk", "1.29.0")
            version("opentelemetryJavaagent", "1.30.0-SNAPSHOT")
            version("opentelemetryJavaagentAlpha", "1.30.0-alpha-SNAPSHOT")

            version("autoservice", "1.1.1")
        }

    }
}