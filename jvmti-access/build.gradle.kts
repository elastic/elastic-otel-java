import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.DockerExistingImage
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.command.CreateContainerResponse
import com.github.dockerjava.api.command.WaitContainerResultCallback
import com.github.dockerjava.api.model.*
import java.io.IOException
import java.util.*

plugins {
    id("java")
    id("com.bmuschko.docker-java-application") version "9.4.0"
}

val jniSrcDir = file("src/main/jni")
val jniBuildDir: Directory = layout.buildDirectory.dir("jni").get()

sourceSets {
  main {
    resources {
      // add a new resource dir that is produced by the task
      srcDir(jniBuildDir)
    }
  }
}

val nativeTargets = listOf(
  NativeTarget(
    "darwin-arm64.so",
    "jni_darwin.Dockerfile",
    "-arch arm64 -std=c++20 -O2 -ftls-model=global-dynamic -fPIC -shared"
  ),
  NativeTarget(
    "darwin-x64.so",
    "jni_darwin.Dockerfile",
    "-arch x86_64 -std=c++20 -O2 -ftls-model=global-dynamic -fPIC -shared"
  ),
  NativeTarget(
    "linux-arm64.so",
    "jni_linux_arm64.Dockerfile",
    "-mtls-dialect=desc -std=c++20 -O2 -ftls-model=global-dynamic -fPIC -shared"
  ),
  NativeTarget(
    "linux-x64.so",
    "jni_linux_x64.Dockerfile",
    "-mtls-dialect=gnu2 -std=c++20 -O2 -ftls-model=global-dynamic -fPIC -shared"
  )
)

task("buildJavaIncludesImage", DockerBuildImage::class) {
  dockerFile.set(file("jni-build/java_includes.Dockerfile"))
  inputDir.set(file("jni-build"))
  images.add("elastic_jni_build_java_includes:latest")

}

val compileJniTask = task("compileJni")
compileJniTask.group = "jni"
tasks.processResources {
  dependsOn(compileJniTask)
}

nativeTargets.forEach {
  val taskSuffix = it.getTaskSuffix();

  val createImageTask = task("buildCompilerImage$taskSuffix", DockerBuildImage::class) {
    dependsOn("buildJavaIncludesImage")
    dockerFile.set(file("jni-build/"+it.dockerfile))
    inputDir.set(file("jni-build"))
  }

  val artifactCompileTask = task("compileJni$taskSuffix", DockerRun::class) {
    dependsOn(createImageTask)
    //compile-java generated the JNI-headers
    dependsOn(tasks.compileJava)

    val artifactName = "elastic-jvmti-${it.artifactSuffix}"
    val actualOutputDir = jniBuildDir.asFile.resolve("elastic-jvmti")
    val artifactFile = actualOutputDir.resolve(artifactName)
    val generatedHeadersDir = layout.buildDirectory.get().dir("generated/sources/headers/java/main")

    inputs.dir(jniSrcDir)
    inputs.dir(generatedHeadersDir)
    outputs.file(artifactFile)

    doFirst {
      actualOutputDir.mkdirs()
      if (artifactFile.exists()) {
        artifactFile.delete()
      }
    }

    targetImageId { createImageTask.imageId.get() }

    binds.put(jniSrcDir.absolutePath, "/jni_src")
    binds.put(generatedHeadersDir.asFile.absolutePath, "/jni_headers")
    binds.put(actualOutputDir.absolutePath, "/jni_dest")
    val args = "${it.compilerArgs} -I /jni_headers -I /jni_src -o /jni_dest/$artifactName /jni_src/*.cpp"
    envVars.put("BUILD_ARGS", args)
  }
  compileJniTask.dependsOn(artifactCompileTask)
}


class NativeTarget(val artifactSuffix: String, val dockerfile: String, val compilerArgs: String) {

  fun getTaskSuffix() : String {
    var suffix = artifactSuffix;
    //remove file suffix
    if(suffix.contains('.')) {
      suffix = suffix.substring(0, suffix.lastIndexOf('.'))
    }
    //replace kebab-case with upper CamelCase
    var result = "";
    for (segment in suffix.split("-")) {
      result += Character.toUpperCase(segment[0]);
      result += segment.substring(1);
    }
    return result;
  }
}

/**
 * Custom task combining creating, running and cleaning up a container.
 */
open class DockerRun : DockerExistingImage() {

  @get:Optional
  @get:Input
  val envVars: MapProperty<String, String> = project.objects.mapProperty(
    String::class.java,
    String::class.java
  )

  @get:Optional
  @get:Input
  val binds: MapProperty<String, String> = project.objects.mapProperty(
    String::class.java,
    String::class.java
  )

  @Throws(IOException::class)
  override fun runRemoteCommand() {
    logger.debug("Creating container")
    val container = createContainer()
    try {

      logger.debug("Starting container with ID '${container.id}'.")
      dockerClient.startContainerCmd(container.id).exec()

      logger.debug("Following logs of container with ID '${container.id}'.")
      followContainerLogs(container)

      val containerWait = dockerClient.waitContainerCmd(container.id)
      val exitCode = containerWait.exec(WaitContainerResultCallback()).awaitStatusCode()

      logger.debug("Container exited with code $exitCode")

      if(exitCode != 0) {
        throw GradleException("Container exited with status code $exitCode, check the logs for details")
      }
    } finally {
      dockerClient.removeContainerCmd(container.id)
        .withForce(true)
        .exec()
    }
  }

  private fun createContainer(): CreateContainerResponse {
    val createContainerCommand = dockerClient.createContainerCmd(imageId.get())
    createContainerCommand.withEnv(
      envVars.get().entries.stream()
        .map { "${it.key}=${it.value}" }
        .toList()
    )
    createContainerCommand.hostConfig.withBinds(binds.get().entries.stream()
      .map { "${it.key}:${it.value}" }
      .map(Bind::parse)
      .toList()
    )
    return createContainerCommand.exec()
  }

  private fun followContainerLogs(container: CreateContainerResponse) {
    val logCommand = dockerClient.logContainerCmd(container.id)
      .withFollowStream(true)
      .withTailAll()
      .withStdErr(true)
      .withStdOut(true)
    logCommand.exec(object : ResultCallback.Adapter<Frame?>() {
      override fun onNext(frame: Frame?) {
        if (frame != null) {
          when (frame.streamType) {
            StreamType.STDOUT, StreamType.RAW -> logger.quiet(
              String(frame.payload).replaceFirst("/\\s+$/".toRegex(), "")
            )

            StreamType.STDERR -> logger.error(
              String(frame.payload).replaceFirst("/\\s+$/".toRegex(), "")
            )

            else -> {}
          }
        }
        super.onNext(frame)
      }
    }).awaitCompletion();
  }

}

