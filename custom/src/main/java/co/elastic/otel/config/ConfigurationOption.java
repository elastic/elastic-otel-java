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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Class definition is still in progress, this is a partial reconstruction of the stagemonitor
 * ConfigurationOption, to be completed as we fill out the configurations already defined in the
 * original agent and decide what we need and what can be implemented less abstractly
 *
 * @param <T>
 */
public class ConfigurationOption<T> {
  private final boolean dynamic;
  private final boolean sensitive;
  private final String key;
  private final List<String> aliasKeys;
  private final List<String> allKeys;
  private final String label;
  private final String description;
  private final T defaultValue;
  private final List<String> tags;
  private final List<Validator<T>> validators;
  private final List<ChangeListener<T>> changeListeners;
  private final boolean required;
  private final String defaultValueAsString;
  private final String configurationCategory;
  private final ValueConverter<T> valueConverter;
  private final Class<? super T> valueType;
  // key: validOptionAsString, value: label
  private final Map<String, String> validOptions;
  private String valueAsString;
  private T value;
  private List<ConfigurationSource> configurationSources;
  private String nameOfCurrentConfigurationSource;
  private String errorMessage;
  //        private ConfigurationRegistry configuration;
  private String usedKey;

  protected ConfigurationOption(
      boolean dynamic,
      boolean sensitive,
      String key,
      String label,
      String description,
      T defaultValue,
      String configurationCategory,
      final ValueConverter<T> valueConverter,
      Class<? super T> valueType,
      List<String> tags,
      boolean required,
      List<ChangeListener<T>> changeListeners,
      List<Validator<T>> validators,
      List<String> aliasKeys,
      final Map<String, String> validOptions) {
    this.dynamic = dynamic;
    this.key = key;
    this.aliasKeys = aliasKeys;
    this.label = label;
    this.description = description;
    this.defaultValue = defaultValue;
    this.tags = tags;
    validators = new ArrayList<Validator<T>>(validators);
    if (validOptions != null) {
      this.validOptions =
          Collections.unmodifiableMap(new LinkedHashMap<String, String>(validOptions));
      //      validators.add(new ValidOptionValidator<T>(validOptions.keySet(), valueConverter));
    } else {
      this.validOptions = null;
    }
    this.validators = Collections.unmodifiableList(new ArrayList<Validator<T>>(validators));
    this.defaultValueAsString = null; // valueConverter.toString(defaultValue);
    this.configurationCategory = configurationCategory;
    this.valueConverter = valueConverter;
    this.valueType = valueType;
    this.sensitive = sensitive;
    this.required = required;
    this.changeListeners = new ArrayList<ChangeListener<T>>(changeListeners);
    //    setToDefault();
    final ArrayList<String> tempAllKeys = new ArrayList<String>(aliasKeys.size() + 1);
    tempAllKeys.add(key);
    tempAllKeys.addAll(aliasKeys);
    this.allKeys = Collections.unmodifiableList(tempAllKeys);
  }

  public static ConfigurationOptionBuilder unspecifiedOption() {
    return new ConfigurationOptionBuilder<Object>();
  }

  public boolean isImplemented() {
    return description != null;
  }

  public String getKey() {
    return key;
  }

  public boolean reconcilesTo(ConfigurationOption<?> option) {
    return key.equals(option.key);
  }

  public static class ConfigurationOptionBuilder<T> {
    private String key;
    private String category;
    private String description;
    private boolean dynamic;
    private String[] tags;
    private boolean required;
    private T defaultValue;
    private boolean sensitive;
    private String label;
    private String configurationCategory;
    private ValueConverter<T> valueConverter;
    private Class<? super T> valueType;
    private List<ChangeListener<T>> changeListeners = new ArrayList<ChangeListener<T>>();
    private List<Validator<T>> validators = new ArrayList<Validator<T>>();
    private String[] aliasKeys = new String[0];
    private Map<String, String> validOptions;

    public ConfigurationOptionBuilder key(String key) {
      this.key = key;
      return this;
    }

    public ConfigurationOptionBuilder configurationCategory(String category) {
      this.category = category;
      return this;
    }

    public ConfigurationOptionBuilder description(String description) {
      this.description = description;
      return this;
    }

    public ConfigurationOptionBuilder dynamic(boolean dynamic) {
      this.dynamic = dynamic;
      return this;
    }

    public ConfigurationOptionBuilder tags(String... tags) {
      this.tags = tags;
      return this;
    }

    public ConfigurationOption<T> buildWithDefault(T defaultValue) {
      this.required = true;
      this.defaultValue = defaultValue;
      return build();
    }

    public ConfigurationOption<T> build() {
      return new ConfigurationOption<T>(
          dynamic,
          sensitive,
          key,
          label,
          description,
          defaultValue,
          configurationCategory,
          valueConverter,
          valueType,
          Arrays.asList(tags),
          required,
          changeListeners,
          validators,
          Arrays.asList(aliasKeys),
          validOptions);
    }

    public ConfigurationOption<T> buildNotEnabled() {
      return new ConfigurationOption<T>(
          false,
          false,
          key,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          false,
          new ArrayList<>(),
          new ArrayList<>(),
          new ArrayList<>(),
          null);
    }
  }

  public interface ValueConverter<T> {}

  public interface Validator<T> {}

  public interface ChangeListener<T> {}

  public interface ConfigurationSource {}
}
