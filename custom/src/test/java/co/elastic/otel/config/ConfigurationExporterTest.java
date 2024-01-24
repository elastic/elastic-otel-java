package co.elastic.otel.config;

import static org.assertj.core.api.Assertions.assertThat;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ConfigurationExporterTest {

  private Path renderedDocumentationPath;

  @BeforeEach
  void setUp() {
    renderedDocumentationPath = Paths.get("../docs/configuration.asciidoc");
  }

  /**
   * This test compares the current state of the configuration docs with the auto-generated documentation and fails if there is a
   * mismatch. As a side effect, it can overwrite the docs with the generated ones, if this capability is enabled through the
   * {@code elastic.apm.overwrite.config.docs} system property.
   */
  @Test
  void testGeneratedConfigurationDocsAreUpToDate() throws IOException, TemplateException {
    // Currently generated documentation is only the mapping of old agent to opentelemetry.
    // It's straightforward to add more with this mechanism
    String renderedDocumentation = renderDocumentation(new LegacyConfigurations().getAllImplementedOptions());
    String currentDocumentation = new String(Files.readAllBytes(this.renderedDocumentationPath), StandardCharsets.UTF_8);

    if (Boolean.parseBoolean(System.getProperty("elastic.otel.apm.overwrite.config.docs", Boolean.FALSE.toString()))) {
      // overwrite the current documentation when enabled
      Files.write(renderedDocumentationPath, renderedDocumentation.getBytes(StandardCharsets.UTF_8));
    }

    assertThat(renderedDocumentation)
        .withFailMessage("The rendered configuration documentation (/docs/configuration.asciidoc) is not up-to-date.\n" +
            "If you see this error, it means you have to execute the tests locally with overwrite enabled " +
            "(gradlew.bat clean :custom:test --tests \"*ConfigurationExporterTest\" -Pelastic.otel.apm.overwrite.config.docs=true) " +
            "which will update the rendered docs (and then you probably need to commit the change).\n")
        .isEqualTo(currentDocumentation);
  }

  static String renderDocumentation(List<ConfigurationOption> configurationRegistry) throws IOException, TemplateException {
    Configuration cfg = new Configuration(Configuration.VERSION_2_3_27);
    cfg.setClassLoaderForTemplateLoading(ConfigurationExporterTest.class.getClassLoader(), "/");
    cfg.setDefaultEncoding("UTF-8");
    cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    cfg.setLogTemplateExceptions(false);

    Template temp = cfg.getTemplate("configuration.asciidoc.ftl");
    StringWriter tempRenderedFile = new StringWriter();
    tempRenderedFile.write("////\n" +
        "This file is auto generated\n" +
        "\n" +
        "Please only make changes in configuration.asciidoc.ftl\n" +
        "////\n");
      final Map<String, List<ConfigurationOption>> optionsByCategory = new HashMap<>();
    optionsByCategory.put("Elastic to Opentelemetry mapping", configurationRegistry);
    temp.process(Map.of(
        "config", optionsByCategory,
        "keys", configurationRegistry.stream().map(ConfigurationOption::getKey).sorted().collect(
            Collectors.toList())
    ), tempRenderedFile);

    return tempRenderedFile.toString();
  }

}

