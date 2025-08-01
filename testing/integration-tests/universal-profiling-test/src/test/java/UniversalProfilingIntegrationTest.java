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
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

@DisabledOnOs(OS.WINDOWS)
public class UniversalProfilingIntegrationTest {

  @Test
  public void checkIntegrationActive() throws IOException {
    // We verify that the integration is running by checking for the presence of the socket
    String socketDir =
        System.getProperty("elastic.otel.universal.profiling.integration.socket.dir");
    List<String> files =
        Files.list(Paths.get(socketDir))
            .map(Path::getFileName)
            .map(Path::toString)
            .collect(Collectors.toList());
    assertThat(files).hasSize(1);
    assertThat(files.get(0)).startsWith("essock");
  }
}
