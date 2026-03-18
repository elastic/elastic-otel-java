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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts library metadata from a JAR file using three sources in priority order:
 * <ol>
 *   <li>META-INF/maven/[groupId]/[artifactId]/pom.properties - most reliable for Maven artifacts
 *   <li>META-INF/MANIFEST.MF - Implementation-Title / Implementation-Version
 *   <li>Filename pattern - name-version.jar best-effort parse
 * </ol>
 */
public final class JarMetadataExtractor {

  private static final Logger logger = Logger.getLogger(JarMetadataExtractor.class.getName());

  /**
   * Matches {@code name-version.jar} patterns where version starts with a digit.
   * Handles common separators, e.g. guava-32.1.3-jre, log4j-core-2.20.0.
   */
  static final Pattern FILENAME_VERSION_PATTERN =
      Pattern.compile("^(.+?)[-_](\\d[\\w.\\-]*)$");

  private JarMetadataExtractor() {}

  /**
   * Extracts {@link JarMetadata} from the given JAR file path.
   *
   * @param jarPath absolute filesystem path to the JAR
   * @param classloaderName class name of the classloader that triggered the discovery
   * @return metadata, or {@code null} if the file cannot be opened
   */
  public static JarMetadata extract(String jarPath, String classloaderName) {
    File file = new File(jarPath);
    if (!file.exists() || !file.isFile()) {
      return null;
    }

    String groupId = "";
    String artifactId = "";
    String version = "";
    String title = "";

    try (JarFile jar = new JarFile(file, false /* no signature verification */)) {
      // Priority 1: pom.properties -- most reliable source for groupId + artifactId + version
      Properties pomProps = findPomProperties(jar);
      if (pomProps != null) {
        groupId = trimToEmpty(pomProps.getProperty("groupId"));
        artifactId = trimToEmpty(pomProps.getProperty("artifactId"));
        version = trimToEmpty(pomProps.getProperty("version"));
      }

      // Priority 2: MANIFEST.MF -- fills gaps when pom.properties is absent or incomplete
      if (version.isEmpty() || artifactId.isEmpty()) {
        Manifest manifest = jar.getManifest();
        if (manifest != null) {
          Attributes attrs = manifest.getMainAttributes();
          if (title.isEmpty()) {
            title = trimToEmpty(attrs.getValue("Implementation-Title"));
          }
          if (version.isEmpty()) {
            version = trimToEmpty(attrs.getValue("Implementation-Version"));
            if (version.isEmpty()) {
              version = trimToEmpty(attrs.getValue("Specification-Version"));
            }
          }
          // Bundle-SymbolicName is common for OSGi bundles -- use as last-resort artifactId
          if (artifactId.isEmpty()) {
            String bundle = trimToEmpty(attrs.getValue("Bundle-SymbolicName"));
            // Strip OSGi directives, e.g. "com.example.foo;singleton:=true"
            int semi = bundle.indexOf(';');
            artifactId = semi >= 0 ? bundle.substring(0, semi).trim() : bundle;
          }
        }
      }
    } catch (IOException e) {
      logger.log(Level.FINE, "SCA: cannot open JAR for metadata extraction: " + jarPath, e);
      return null;
    }

    // Priority 3: filename -- best-effort version extraction and last-resort name
    String baseName = baseNameOf(file.getName());
    if (artifactId.isEmpty()) {
      Matcher m = FILENAME_VERSION_PATTERN.matcher(baseName);
      if (m.matches()) {
        artifactId = m.group(1);
        if (version.isEmpty()) {
          version = m.group(2);
        }
      }
      // Note: when the filename does not match the version pattern we leave artifactId empty
      // so that 'title' from the MANIFEST (if present) is preferred over the raw filename below.
    }

    // Name resolution: artifactId > MANIFEST title > filename (in that order)
    String name;
    if (!artifactId.isEmpty()) {
      name = artifactId;
    } else if (!title.isEmpty()) {
      name = title;
    } else {
      name = baseName;
    }

    String purl = buildPurl(groupId, artifactId, version);
    String sha256 = computeSha256(file);

    return new JarMetadata(name, version, groupId, purl, jarPath, sha256, classloaderName);
  }

  /**
   * Finds and loads the first pom.properties entry under META-INF/maven/ in the JAR.
   *
   * @return loaded {@link Properties}, or {@code null} if not found
   */
  static Properties findPomProperties(JarFile jar) throws IOException {
    Enumeration<JarEntry> entries = jar.entries();
    while (entries.hasMoreElements()) {
      JarEntry entry = entries.nextElement();
      if (entry.isDirectory()) {
        continue;
      }
      String name = entry.getName();
      // META-INF/maven/{groupId}/{artifactId}/pom.properties
      if (name.startsWith("META-INF/maven/") && name.endsWith("/pom.properties")) {
        Properties props = new Properties();
        try (InputStream in = jar.getInputStream(entry)) {
          props.load(in);
        }
        return props;
      }
    }
    return null;
  }

  /**
   * Builds a Package URL string in {@code pkg:maven/{groupId}/{artifactId}@{version}} format.
   * Returns an empty string when {@code artifactId} is empty.
   */
  static String buildPurl(String groupId, String artifactId, String version) {
    if (artifactId.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder("pkg:maven/");
    if (!groupId.isEmpty()) {
      sb.append(groupId).append('/');
    }
    sb.append(artifactId);
    if (!version.isEmpty()) {
      sb.append('@').append(version);
    }
    return sb.toString();
  }

  /** Computes the hex-encoded SHA-256 digest of the given file. Returns empty on I/O error. */
  static String computeSha256(File file) {
    MessageDigest md;
    try {
      md = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      // SHA-256 is mandated by the Java spec -- should never happen
      logger.log(Level.WARNING, "SCA: SHA-256 algorithm unavailable", e);
      return "";
    }
    try (FileInputStream in = new FileInputStream(file)) {
      byte[] buf = new byte[8192];
      int n;
      while ((n = in.read(buf)) != -1) {
        md.update(buf, 0, n);
      }
    } catch (IOException e) {
      logger.log(Level.FINE, "SCA: could not read JAR for SHA-256: " + file.getPath(), e);
      return "";
    }
    byte[] digest = md.digest();
    StringBuilder hex = new StringBuilder(digest.length * 2);
    for (byte b : digest) {
      hex.append(String.format("%02x", b));
    }
    return hex.toString();
  }

  /** Strips the {@code .jar} suffix from a filename. */
  private static String baseNameOf(String fileName) {
    if (fileName.endsWith(".jar")) {
      return fileName.substring(0, fileName.length() - 4);
    }
    return fileName;
  }

  private static String trimToEmpty(String s) {
    return s == null ? "" : s.trim();
  }
}
