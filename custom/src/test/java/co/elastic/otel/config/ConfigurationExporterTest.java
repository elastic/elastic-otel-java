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
package co.elastic.otel.config;

import static org.assertj.core.api.Assertions.assertThat;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
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

  private Path currentDocumentationPath;

  @BeforeEach
  void setUp() {
    currentDocumentationPath = Paths.get("../docs/migrate.md");
  }

  /**
   * This test compares the current state of the configuration docs with the auto-generated
   * documentation and fails if there is a mismatch. As a side effect, it can overwrite the docs
   * with the generated ones, if this capability is enabled through the {@code
   * elastic.overwrite.config.docs} system property.
   */
  @Test
  void testGeneratedConfigurationDocsAreUpToDate() throws IOException, TemplateException {
    // Currently generated documentation is only the mapping of old agent to opentelemetry.
    // It's straightforward to add more with this mechanism
    String renderedDocumentation =
        renderDocumentation(new LegacyConfigurations().getAllImplementedOptions());
    String currentDocumentation =
        new String(Files.readAllBytes(this.currentDocumentationPath), StandardCharsets.UTF_8);

    if (Boolean.parseBoolean(
        System.getProperty("elastic.otel.overwrite.config.docs", Boolean.FALSE.toString()))) {
      // overwrite the current documentation when enabled
      Files.write(currentDocumentationPath, renderedDocumentation.getBytes(StandardCharsets.UTF_8));
    }

    assertThat(renderedDocumentation)
        .withFailMessage(
            "The rendered configuration documentation (/docs/migrate.md) is not up-to-date.\n"
                + "If you see this error, it means you have to execute the tests locally with overwrite enabled "
                + "(gradlew.bat clean :custom:test --tests \"*ConfigurationExporterTest\" -Pelastic.otel.overwrite.config.docs=true) "
                + "which will update the rendered docs (and then you probably need to commit the change).\n")
        .isEqualTo(currentDocumentation);
  }

  static String renderDocumentation(List<ConfigurationOption> configurationRegistry)
      throws IOException, TemplateException {
    Configuration cfg = new Configuration(Configuration.VERSION_2_3_27);
    cfg.setClassLoaderForTemplateLoading(ConfigurationExporterTest.class.getClassLoader(), "/");
    cfg.setDefaultEncoding("UTF-8");
    cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    cfg.setLogTemplateExceptions(false);

    Template temp = cfg.getTemplate("migrate.md.ftl");
    StringWriter tempRenderedFile = new StringWriter();
    tempRenderedFile.write(
        "[[migrate]]\n"
            + "== Migrate to the Elastic distribution\n\n"
            + "////\n"
            + "This file is auto generated. Please only make changes in `migrate.md.ftl`\n"
            + "////\n");
    final Map<String, List<ConfigurationOption>> optionsByCategory = new HashMap<>();
    optionsByCategory.put("Elastic to OpenTelemetry mapping", configurationRegistry);
    Map<String, Object> map = new HashMap<>();
    map.put("config", optionsByCategory);
    map.put(
        "keys",
        configurationRegistry.stream()
            .map(ConfigurationOption::getKey)
            .sorted()
            .collect(Collectors.toList()));
    temp.process(map, tempRenderedFile);

    return tempRenderedFile.toString();
  }
}
