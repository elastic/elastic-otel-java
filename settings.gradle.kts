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
include("resources")
include("resources:repackaged")
include("smoke-tests")
include("smoke-tests:test-app")
include("smoke-tests:test-app-war")
include("testing:agent-for-testing")

dependencyResolutionManagement {
    versionCatalogs {
        create("gradlePlugins") {
          from(files("gradle/gradlePlugins.toml"))
        }
        create("libraries") {
          from(files("gradle/libraries.toml"))
        }
    }
}
