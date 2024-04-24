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

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;

public class JvmtiAccess {

  private static final Logger logger = Logger.getLogger(JvmtiAccess.class.getName());

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
      throw new IllegalStateException("Agent could not be initialized");
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
      throw new RuntimeException("Elastic JVMTI Agent returned error code " + returnCode);
    }
  }

  private static void loadNativeLibrary() {
    String os = System.getProperty("os.name").toLowerCase();
    String arch = System.getProperty("os.arch").toLowerCase();
    String libraryName;
    if (os.contains("linux")) {
      if (arch.contains("arm") || arch.contains("aarch32")) {
        throw new IllegalStateException("Unsupported architecture for Linux: " + arch);
      } else if (arch.contains("aarch")) {
        libraryName = "linux-arm64";
      } else if (arch.contains("64")) {
        libraryName = "linux-x64";
      } else {
        throw new IllegalStateException("Unsupported architecture for Linux: " + arch);
      }
    } else if (os.contains("mac")) {
      if (arch.contains("aarch")) {
        libraryName = "darwin-arm64";
      } else {
        libraryName = "darwin-x64";
      }
    } else {
      throw new IllegalStateException("Native agent does not work on " + os);
    }

    String libraryDirectory = System.getProperty("java.io.tmpdir");
    libraryName = "elastic-jvmti-" + libraryName;
    Path file =
        ResourceExtractionUtil.extractResourceToDirectory(
            "elastic-jvmti/" + libraryName + ".so",
            libraryName,
            ".so",
            Paths.get(libraryDirectory));
    System.load(file.toString());
  }
}
