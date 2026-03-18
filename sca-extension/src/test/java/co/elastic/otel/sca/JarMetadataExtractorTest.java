/*
 * Licensed to Elasticsearch B.V. under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch B.V. licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package co.elastic.otel.sca;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JarMetadataExtractorTest {

  @TempDir File tempDir;

  // ---- pom.properties -------------------------------------------------------

  @Test
  void extractFromPomProperties() throws IOException {
    File jar = buildJar("my-artifact-1.2.3.jar", jarOut -> {
      addPomProperties(jarOut, "com.example", "my-artifact", "1.2.3");
    });

    JarMetadata meta = JarMetadataExtractor.extract(jar.getAbsolutePath(), "testloader");

    assertThat(meta).isNotNull();
    assertThat(meta.groupId).isEqualTo("com.example");
    assertThat(meta.name).isEqualTo("my-artifact");
    assertThat(meta.version).isEqualTo("1.2.3");
    assertThat(meta.purl).isEqualTo("pkg:maven/com.example/my-artifact@1.2.3");
    assertThat(meta.classloaderName).isEqualTo("testloader");
  }

  @Test
  void pomPropertiesTakesPrecedenceOverManifest() throws IOException {
    File jar = buildJar("artifact.jar", jarOut -> {
      addPomProperties(jarOut, "com.pom", "pom-artifact", "2.0");
      addManifest(jarOut, "Manifest-Artifact", "9.9.9", null);
    });

    JarMetadata meta = JarMetadataExtractor.extract(jar.getAbsolutePath(), "");

    assertThat(meta.groupId).isEqualTo("com.pom");
    assertThat(meta.name).isEqualTo("pom-artifact");
    assertThat(meta.version).isEqualTo("2.0");
  }

  // ---- MANIFEST.MF ----------------------------------------------------------

  @Test
  void extractFromManifest() throws IOException {
    File jar = buildJar("manifest-only.jar", jarOut -> {
      addManifest(jarOut, "My Library", "3.1.0", null);
    });

    JarMetadata meta = JarMetadataExtractor.extract(jar.getAbsolutePath(), "");

    assertThat(meta).isNotNull();
    assertThat(meta.version).isEqualTo("3.1.0");
    // name falls back to Implementation-Title when no artifactId
    assertThat(meta.name).isEqualTo("My Library");
  }

  @Test
  void manifestSpecificationVersionUsedWhenImplementationVersionAbsent() throws IOException {
    File jar = buildJar("spec-version.jar", jarOut -> {
      addManifest(jarOut, "SpecLib", null, "4.0");
    });

    JarMetadata meta = JarMetadataExtractor.extract(jar.getAbsolutePath(), "");

    assertThat(meta).isNotNull();
    assertThat(meta.version).isEqualTo("4.0");
  }

  // ---- Filename fallback ----------------------------------------------------

  @Test
  void extractVersionFromFilename() throws IOException {
    File jar = buildJar("guava-32.1.3-jre.jar", jarOut -> { /* no metadata */ });

    JarMetadata meta = JarMetadataExtractor.extract(jar.getAbsolutePath(), "");

    assertThat(meta).isNotNull();
    assertThat(meta.name).isEqualTo("guava");
    assertThat(meta.version).isEqualTo("32.1.3-jre");
    assertThat(meta.purl).isEqualTo("pkg:maven/guava@32.1.3-jre");
  }

  @Test
  void noVersionInFilename() throws IOException {
    File jar = buildJar("tools.jar", jarOut -> { /* no metadata */ });

    JarMetadata meta = JarMetadataExtractor.extract(jar.getAbsolutePath(), "");

    assertThat(meta).isNotNull();
    assertThat(meta.name).isEqualTo("tools");
    assertThat(meta.version).isEmpty();
  }

  // ---- pURL construction ----------------------------------------------------

  @Test
  void purlWithoutGroupId() {
    assertThat(JarMetadataExtractor.buildPurl("", "my-lib", "1.0"))
        .isEqualTo("pkg:maven/my-lib@1.0");
  }

  @Test
  void purlWithoutVersion() {
    assertThat(JarMetadataExtractor.buildPurl("org.example", "lib", ""))
        .isEqualTo("pkg:maven/org.example/lib");
  }

  @Test
  void purlEmptyWhenNoArtifactId() {
    assertThat(JarMetadataExtractor.buildPurl("org.example", "", "1.0")).isEmpty();
  }

  // ---- SHA-256 --------------------------------------------------------------

  @Test
  void sha256IsDeterministic() throws IOException {
    File jar = buildJar("stable.jar", jarOut -> {
      addPomProperties(jarOut, "org.stable", "stable", "1.0");
    });

    String first = JarMetadataExtractor.computeSha256(jar);
    String second = JarMetadataExtractor.computeSha256(jar);

    assertThat(first).isNotEmpty().hasSize(64).isEqualTo(second);
  }

  @Test
  void sha256DiffersForDifferentContent() throws IOException {
    File jar1 = buildJar("a.jar", jarOut -> addPomProperties(jarOut, "g", "a", "1"));
    File jar2 = buildJar("b.jar", jarOut -> addPomProperties(jarOut, "g", "b", "2"));

    assertThat(JarMetadataExtractor.computeSha256(jar1))
        .isNotEqualTo(JarMetadataExtractor.computeSha256(jar2));
  }

  // ---- Missing file ---------------------------------------------------------

  @Test
  void returnsNullForNonExistentFile() {
    JarMetadata meta = JarMetadataExtractor.extract("/does/not/exist.jar", "");
    assertThat(meta).isNull();
  }

  // ---- Helpers --------------------------------------------------------------

  @FunctionalInterface
  interface JarPopulator {
    void populate(JarOutputStream jarOut) throws IOException;
  }

  private File buildJar(String name, JarPopulator populator) throws IOException {
    File jar = new File(tempDir, name);
    try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jar))) {
      populator.populate(jos);
    }
    return jar;
  }

  private static void addPomProperties(
      JarOutputStream jos, String groupId, String artifactId, String version) throws IOException {
    jos.putNextEntry(
        new JarEntry("META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties"));
    Properties props = new Properties();
    props.setProperty("groupId", groupId);
    props.setProperty("artifactId", artifactId);
    props.setProperty("version", version);
    props.store(jos, null);
    jos.closeEntry();
  }

  private static void addManifest(
      JarOutputStream jos,
      String implTitle,
      String implVersion,
      String specVersion) throws IOException {
    Manifest mf = new Manifest();
    Attributes attrs = mf.getMainAttributes();
    attrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
    if (implTitle != null) {
      attrs.put(Attributes.Name.IMPLEMENTATION_TITLE, implTitle);
    }
    if (implVersion != null) {
      attrs.put(Attributes.Name.IMPLEMENTATION_VERSION, implVersion);
    }
    if (specVersion != null) {
      attrs.put(Attributes.Name.SPECIFICATION_VERSION, specVersion);
    }
    jos.putNextEntry(new JarEntry(JarFile.MANIFEST_NAME));
    mf.write(jos);
    jos.closeEntry();
  }
}
