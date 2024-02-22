import java.io.FileInputStream
import java.util.*

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            name = "mavenCentralSnapshots"
            url = uri("https://oss.sonatype.org/content/repositories/snapshots")
        }
    }
}

rootProject.name = "elastic-otel-java"

include("agent")
include("bootstrap")
include("custom")
include("instrumentation")
include("inferred-spans")
include("resources")
include("resources:repackaged")
include("smoke-tests")
include("smoke-tests:test-app")
include("smoke-tests:test-app-war")
include("testing:agent-for-testing")
include("jvmti-access")
include("common")
include("testing-common")
include("universal-profiling-integration")
include("testing:integration-tests:inferred-spans-test")

dependencyResolutionManagement {
    versionCatalogs {
        create("catalog") {
          // the version catalog is currently limited to a single file due to dependabot
          // see https://github.com/dependabot/dependabot-core/issues/8079 for details.
          // Also, only the 'gradle/libs.versions.toml' path is supported for now.
          from(files("gradle/libs.versions.toml"))
        }
    }
}
