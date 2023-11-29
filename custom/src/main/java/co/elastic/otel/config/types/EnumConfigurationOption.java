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
package co.elastic.otel.config.types;

import co.elastic.otel.config.ConfigurationOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class EnumConfigurationOption<T extends Enum<T>> extends ConfigurationOption<T> {
  private final Class<T> enumClass;

  protected EnumConfigurationOption(
      boolean dynamic,
      boolean sensitive,
      String key,
      String label,
      String description,
      T defaultValue,
      String configurationCategory,
      List<String> tags,
      boolean required,
      List<ChangeListener<T>> changeListeners,
      List<Validator<T>> validators,
      List<String> aliasKeys,
      Map<String, String> validOptions,
      ChangesFromV1Definitions changesFromV1Definitions,
      Class<T> enumClass) {
    super(
        dynamic,
        sensitive,
        key,
        label,
        description,
        defaultValue,
        configurationCategory,
        null,
        null,
        tags,
        required,
        changeListeners,
        validators,
        aliasKeys,
        validOptions,
        changesFromV1Definitions);
    this.enumClass = enumClass;
  }

  @Override
  public T convert(String name) throws IllegalArgumentException {
    return Enum.valueOf(enumClass, name);
  }

  public static class EnumConfigurationOptionBuilder<T extends Enum<T>>
      extends ConfigurationOptionBuilder<T> {

    private final Class<T> enumClass;

    public EnumConfigurationOptionBuilder(Class<T> enumClass) {
      this.enumClass = enumClass;
    }

    public EnumConfigurationOption<T> build() {
      return new EnumConfigurationOption<>(
          dynamic,
          sensitive,
          key,
          label,
          description,
          defaultValue,
          configurationCategory,
          tags == null ? new ArrayList<>() : Arrays.asList(tags),
          required,
          changeListeners,
          validators,
          aliasKeys == null ? new ArrayList<>() : Arrays.asList(aliasKeys),
          validOptions,
          changesFromV1Definitions,
          enumClass);
    }
  }
}
