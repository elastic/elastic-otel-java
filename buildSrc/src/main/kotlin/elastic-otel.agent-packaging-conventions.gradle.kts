import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.TransformerContext
import org.apache.tools.zip.ZipEntry
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes

plugins {
  id("java")
  id("com.github.johnrengelman.shadow")
}


// This plugin holds the logic of how the distribution is packaged into the final javaagent jar
// This is used at two places: For packaging the actual agent and the agent-for-testing
// Both only should differ in the upstream agent they use


// this configuration collects libs that will be placed in the bootstrap classloader
val bootstrapLibs: Configuration by configurations.creating {
  isCanBeConsumed = false
}
val javaagentLibs: Configuration by configurations.creating {
  isCanBeConsumed = false
}
val upstreamAgent: Configuration by configurations.creating {
  isCanBeConsumed = false
}

dependencies {
  bootstrapLibs(project(":bootstrap"))

  bootstrapLibs(project(":agent:entrypoint"))

  javaagentLibs(project(":custom"))
}

fun relocatePackages( shadowJar : ShadowJar) {
  // rewrite dependencies calling Logger.getLogger
  shadowJar.relocate("java.util.logging.Logger", "io.opentelemetry.javaagent.bootstrap.PatchLogger")

  // prevents conflict with library instrumentation, since these classes live in the bootstrap class loader
  shadowJar.relocate("io.opentelemetry.instrumentation", "io.opentelemetry.javaagent.shaded.instrumentation") {
    // Exclude resource providers since they live in the agent class loader
    exclude("io.opentelemetry.instrumentation.resources.*")
    exclude("io.opentelemetry.instrumentation.spring.resources.*")
  }

  // relocate(OpenTelemetry API) since these classes live in the bootstrap class loader
  shadowJar.relocate("io.opentelemetry.api", "io.opentelemetry.javaagent.shaded.io.opentelemetry.api")
  shadowJar.relocate("io.opentelemetry.semconv", "io.opentelemetry.javaagent.shaded.io.opentelemetry.semconv")
  shadowJar.relocate("io.opentelemetry.context", "io.opentelemetry.javaagent.shaded.io.opentelemetry.context")
  shadowJar.relocate("io.opentelemetry.extension.incubator", "io.opentelemetry.javaagent.shaded.io.opentelemetry.extension.incubator")

  // relocate the OpenTelemetry extensions that are used by instrumentation modules
  // these extensions live in the AgentClassLoader, and are injected into the user's class loader
  // by the instrumentation modules that use them
  shadowJar.relocate("io.opentelemetry.extension.aws", "io.opentelemetry.javaagent.shaded.io.opentelemetry.extension.aws")
  shadowJar.relocate("io.opentelemetry.extension.kotlin", "io.opentelemetry.javaagent.shaded.io.opentelemetry.extension.kotlin")
}

tasks {
  jar {
    enabled = false
    dependsOn(shadowJar)
  }

  // building the final javaagent jar is done in 3 steps:

  // 1. all distro specific javaagent libs are relocated
  val relocateJavaagentLibs = register<ShadowJar>("relocateJavaagentLibs") {
    configurations = listOf(javaagentLibs)

    duplicatesStrategy = DuplicatesStrategy.FAIL

    archiveFileName.set("javaagentLibs-relocated.jar")

    mergeServiceFiles()
    exclude("**/module-info.class")
    relocatePackages(this)

    // exclude known bootstrap dependencies - they can't appear in the inst/ directory

    exclude("io/opentelemetry/api/")
    // SDK should not be included, but we have one extra addition to it
    exclude { it.path.startsWith("io/opentelemetry/sdk") and !it.name.contains("FieldBackedSpanValueStorageProvider.class") }
    exclude("io/opentelemetry/semconv/")
    exclude("io/opentelemetry/context/")
    exclude("io/opentelemetry/internal/")
    // metrics advice API
    exclude("io/opentelemetry/extension/incubator/")
  }

  // 2. the distro javaagent libs are then isolated - moved to the inst/ directory
  // having a separate task for isolating javaagent libs is required to avoid duplicates with the upstream javaagent
  // duplicatesStrategy in shadowJar won't be applied when adding files with with(CopySpec) because each CopySpec has
  // its own duplicatesStrategy
  val isolateJavaagentLibs = register<Copy>("isolateJavaagentLibs") {
    dependsOn(relocateJavaagentLibs)
    relocateJavaagentLibs.get().outputs.files.forEach {
      from(zipTree(it)) {
        into("inst")
        rename("^(.*)\\.class\$", "\$1.classdata")
        // Rename LICENSE file since it clashes with license dir on non-case sensitive FSs (i.e. Mac)
        rename("^LICENSE\$", "LICENSE.renamed")
        exclude("META-INF/INDEX.LIST")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.SF")
      }
    }
    into("build/isolated/javaagentLibs")
  }


  // This transformer injects a new Field into the Opentelemetry SdkSpan class to be used
  // as efficient storage for co.elastic.otel.common.SpanValues
  // Check the FieldBackedSpanValueStorageProvider for details
  val injectSpanValueFieldTransformer = object: com.github.jengelman.gradle.plugins.shadow.transformers.Transformer {

    @Internal
    val SDK_SPAN_CLASS_FILE = "inst/io/opentelemetry/sdk/trace/SdkSpan.classdata"
    @Internal
    val FIELD_NAME = "\$elasticSpanValues"

    @Internal
    var bytecode : ByteArray? = null;

    override fun getName(): String {
      return "SpanValue field injector into Otel SdkSpan"
    }

    override fun canTransformResource(element: FileTreeElement): Boolean {
      return element.name.equals(SDK_SPAN_CLASS_FILE)
    }

    override fun transform(context: TransformerContext) {
      if(bytecode != null) {
        throw IllegalStateException("Multiple SdkSpan classes detected")
      }

      val inputStream = context.getIs()
      val reader = ClassReader(inputStream)
      val writer = ClassWriter(reader, 0)
      val visitor = object : ClassVisitor(Opcodes.ASM9, writer) {
        override fun visitEnd() {
          val flags = Opcodes.ACC_VOLATILE //package-private visibility
          val fv = writer.visitField(flags , FIELD_NAME, "Ljava/lang/Object;", null ,null)
          fv.visitEnd()
          super.visitEnd()
        }
      }
      reader.accept(visitor, 0);
      bytecode = writer.toByteArray()
      inputStream.close()
    }

    override fun hasTransformedResource(): Boolean {
      return true;
    }

    override fun modifyOutputStream(
      os: org.apache.tools.zip.ZipOutputStream,
      preserveFileTimestamps: Boolean
    ) {
      if(bytecode == null) {
        throw IllegalStateException("Failed to find SdkSpan class, was it moved? Searched for $SDK_SPAN_CLASS_FILE")
      }

      val entry = ZipEntry(SDK_SPAN_CLASS_FILE)
      entry.time = TransformerContext.getEntryTimestamp(preserveFileTimestamps, entry.time)
      os.putNextEntry(entry)
      os.write(bytecode)
    }

  }

  // 3. the relocated and isolated javaagent libs are merged together with the bootstrap libs (which undergo relocation
  // in this task) and the upstream javaagent jar; duplicates are removed
  shadowJar {

    dependsOn(isolateJavaagentLibs)
    configurations = listOf(bootstrapLibs, upstreamAgent)

    from(isolateJavaagentLibs.get().outputs)

    archiveClassifier.set("")

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    mergeServiceFiles {
      include("inst/META-INF/services/*")
    }
    exclude("**/module-info.class")
    relocatePackages(this)
    transform(injectSpanValueFieldTransformer)

    manifest {
      attributes["Main-Class"] = "co.elastic.otel.agent.ElasticAgent"
      attributes["Agent-Class"] = "co.elastic.otel.agent.ElasticAgent"
      attributes["Premain-Class"] = "co.elastic.otel.agent.ElasticAgent"
      attributes["Can-Redefine-Classes"] = "true"
      attributes["Can-Retransform-Classes"] = "true"
      attributes["Implementation-Vendor"] = "Elastic"
      attributes["Implementation-Title"] = "co.elastic.otel:elastic-otel-javaagent"
      attributes["Implementation-Version"] = project.version
      // TODO : add git hash to version for easier support with SCM-Revision
    }


  }
}
