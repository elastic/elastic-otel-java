
// Umbrella task for executing muzzle of all subprojects
// Note that invoking just "/.gradlew muzzle" reports that muzzle was executed,
// but it actually doesn't work, the plugin simply does nothing
// The same applies to executing the gradle task from the working directory of a subproject, like doing it via IntelliJ does
// You must really run exactly "./gradlew clean :instrumentation:muzzle" from the command line to have muzzle actually execute
val instrumentationProjectMuzzle = task("muzzle")

subprojects {
  val subProj = this
  plugins.withId("io.opentelemetry.instrumentation.muzzle-check") {
    instrumentationProjectMuzzle.dependsOn(subProj.tasks.named("muzzle"))
  }
}
