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

/** Immutable value object holding extracted metadata for a single JAR file. */
public final class JarMetadata {

  /** The artifact name (artifactId from Maven, or Implementation-Title, or parsed filename). */
  final String name;

  /** The artifact version string. Empty string if not determinable. */
  final String version;

  /** The Maven groupId. Empty string if not determinable. */
  final String groupId;

  /**
   * Package URL in {@code pkg:maven/{groupId}/{artifactId}@{version}} format. Empty if the
   * artifact name could not be determined.
   */
  final String purl;

  /** The absolute filesystem path to the JAR file. */
  final String jarPath;

  /** Hex-encoded SHA-256 digest of the JAR file bytes. Empty string on I/O error. */
  final String sha256;

  /** The {@link ClassLoader#getClass() class name} of the classloader that first loaded from it. */
  final String classloaderName;

  JarMetadata(
      String name,
      String version,
      String groupId,
      String purl,
      String jarPath,
      String sha256,
      String classloaderName) {
    this.name = name;
    this.version = version;
    this.groupId = groupId;
    this.purl = purl;
    this.jarPath = jarPath;
    this.sha256 = sha256;
    this.classloaderName = classloaderName;
  }

  @Override
  public String toString() {
    return "JarMetadata{"
        + "name='"
        + name
        + '\''
        + ", version='"
        + version
        + '\''
        + ", groupId='"
        + groupId
        + '\''
        + ", purl='"
        + purl
        + '\''
        + '}';
  }
}
