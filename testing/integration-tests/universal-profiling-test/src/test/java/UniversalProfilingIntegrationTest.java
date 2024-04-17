import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

public class UniversalProfilingIntegrationTest {

  @Test
  public void checkIntegrationActive() throws IOException {
    // We verify that the integration is running by checking for the presence of the socket
    String socketDir = System.getProperty(
        "elastic.otel.universal.profiling.integration.socket.dir");
    List<String> files = Files.list(Paths.get(socketDir))
        .map(Path::getFileName)
        .map(Path::toString)
        .collect(Collectors.toList());
    assertThat(files).hasSize(1);
    assertThat(files.get(0)).startsWith("essock");
  }
  
}
