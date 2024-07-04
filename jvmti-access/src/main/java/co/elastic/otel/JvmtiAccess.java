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
package co.elastic.otel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;

public class JvmtiAccess {

  private static final Logger logger = Logger.getLogger(JvmtiAccess.class.getName());

  private static volatile LibraryLookupResult libraryLookupResult = null;

  private enum State {
    NOT_LOADED,
    LOAD_FAILED,
    LOADED,
    INITIALIZED,
    INITIALIZATION_FAILED,
    DESTROY_FAILED
  }

  private static volatile State state = State.NOT_LOADED;

  static void setProfilingCorrelationProcessStorage(@Nullable ByteBuffer storage) {
    ensureInitialized();
    JvmtiAccessImpl.setProcessProfilingCorrelationBuffer0(storage);
  }

  static void setProfilingCorrelationCurrentThreadStorage(@Nullable ByteBuffer storage) {
    ensureInitialized();
    JvmtiAccessImpl.setThreadProfilingCorrelationBuffer0(storage);
  }

  /**
   * Starts the socket for receiving universal profiler messages on the given filepath. Note that
   * the path has a limitation of about 100 characters, see <a
   * href="https://unix.stackexchange.com/questions/367008/why-is-socket-path-length-limited-to-a-hundred-chars">this
   * discussion</a> for details.
   */
  static void startProfilerReturnChannelSocket(String filepath) {
    ensureInitialized();
    checkError(JvmtiAccessImpl.startProfilerReturnChannelSocket0(filepath));
  }

  static void stopProfilerReturnChannelSocket() {
    ensureInitialized();
    checkError(JvmtiAccessImpl.stopProfilerReturnChannelSocket0());
  }

  static int receiveProfilerReturnChannelMessage(ByteBuffer outputBuffer) {
    ensureInitialized();
    int numRead = JvmtiAccessImpl.readProfilerReturnChannelSocketMessage0(outputBuffer);
    if (numRead < 0) {
      throw new IllegalStateException("Native code returned error: " + numRead);
    }
    return numRead;
  }

  public static void ensureInitialized() {
    switch (state) {
      case NOT_LOADED:
      case LOADED:
        doInit();
    }
    if (state != State.INITIALIZED) {
      throw new IllegalStateException("jvmti native library could not be initialized");
    }
  }

  private static synchronized void doInit() {
    switch (state) {
      case NOT_LOADED:
        try {
          loadNativeLibrary();
          state = State.LOADED;
        } catch (Throwable t) {
          logger.log(Level.SEVERE, "Failed to load jvmti native library", t);
          state = State.LOAD_FAILED;
          return;
        }
      case LOADED:
        try {
          // TODO: call an initialization method and check the results
          state = State.INITIALIZED;
        } catch (Throwable t) {
          logger.log(Level.SEVERE, "Failed to initialize jvmti native library", t);
          state = State.INITIALIZATION_FAILED;
          return;
        }
    }
  }

  public static synchronized void destroy() {
    switch (state) {
      case INITIALIZED:
        try {
          UniversalProfilingCorrelation.reset();
          checkError(JvmtiAccessImpl.destroy0());
          state = State.LOADED;
        } catch (Throwable t) {
          logger.log(Level.SEVERE, "Failed to shutdown jvmti native library", t);
          state = State.DESTROY_FAILED;
        }
    }
  }

  private static void checkError(int returnCode) {
    if (returnCode < 0) {
      throw new RuntimeException("Elastic jvmti native library returned error code " + returnCode);
    }
  }

  /**
   * @return null, if the current system is supported. Otherwise a String with a reason why the
   *     system is not supported.
   */
  @Nullable
  public static String getSystemUnsupportedReason() {
    return lookupLibrary().failureReason;
  }

  private static void loadNativeLibrary() {
    LibraryLookupResult lib = lookupLibrary();
    if (lib.failureReason != null) {
      throw new IllegalStateException(lib.failureReason);
    }

    String libraryDirectory = System.getProperty("java.io.tmpdir");
    String libraryName = "elastic-jvmti-" + lib.libraryName;
    Path file =
        ResourceExtractionUtil.extractResourceToDirectory(
            "elastic-jvmti/" + libraryName + ".so",
            libraryName,
            ".so",
            Paths.get(libraryDirectory));
    System.load(file.toString());
  }

  private static LibraryLookupResult lookupLibrary() {
    if (libraryLookupResult == null) {
      libraryLookupResult = doLookupLibrary();
    }
    return libraryLookupResult;
  }

  private static LibraryLookupResult doLookupLibrary() {
    String os = System.getProperty("os.name").toLowerCase();
    String arch = System.getProperty("os.arch").toLowerCase();
    if (os.contains("linux")) {
      String linuxVariant = "linux";
      if (isMusl()) {
        linuxVariant = "linux-musl";
      }
      if (arch.contains("arm") || arch.contains("aarch32")) {
        return LibraryLookupResult.failure("Unsupported architecture for Linux: " + arch);
      } else if (arch.contains("aarch")) {
        return LibraryLookupResult.success(linuxVariant + "-arm64");
      } else if (arch.contains("64")) {
        return LibraryLookupResult.success(linuxVariant + "-x64");
      } else {
        return LibraryLookupResult.failure("Unsupported architecture for Linux: " + arch);
      }
    } else if (os.contains("mac")) {
      if (arch.contains("aarch")) {
        return LibraryLookupResult.success("darwin-arm64");
      } else {
        return LibraryLookupResult.success("darwin-x64");
      }
    } else {
      return LibraryLookupResult.failure("jvmti native library does not work on " + os);
    }
  }

  // The isMusl() and isAlpineLinux() methods are based on the approach taken by the sqlite-jdbc
  // project:
  // https://github.com/xerial/sqlite-jdbc/pull/675
  private static boolean isMusl() {
    Path mapFilesDir = Paths.get("/proc/self/map_files");
    try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(mapFilesDir)) {
      for (Path file : dirStream) {
        try {
          if (file.toRealPath().toString().toLowerCase().contains("musl")) {
            return true;
          }
        } catch (IOException e) {
          // ignore
        }
      }
      return false;
    } catch (Exception ignored) {
      // fall back to checking for alpine linux in the event we're using an older kernel which
      // may not fail the above check
      return isAlpineLinux();
    }
  }

  private static boolean isAlpineLinux() {
    try {
      List<String> lines = Files.readAllLines(Paths.get("/etc/os-release"), StandardCharsets.UTF_8);
      for (String l : lines) {
        if (l.startsWith("ID") && l.contains("alpine")) {
          return true;
        }
      }
    } catch (Exception ignored) {
    }
    return false;
  }

  private static class LibraryLookupResult {

    // Either failureReason or libraryName are not null
    @Nullable final String failureReason;
    @Nullable final String libraryName;

    public LibraryLookupResult(@Nullable String libraryName, @Nullable String failureReason) {
      this.failureReason = failureReason;
      this.libraryName = libraryName;
    }

    static LibraryLookupResult failure(String reason) {
      return new LibraryLookupResult(null, reason);
    }

    static LibraryLookupResult success(String libraryName) {
      return new LibraryLookupResult(libraryName, null);
    }
  }
}
