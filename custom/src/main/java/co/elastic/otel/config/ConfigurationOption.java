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

import co.elastic.otel.config.types.BooleanConfigurationOption;
import co.elastic.otel.config.types.EnumConfigurationOption;
import co.elastic.otel.config.types.StringConfigurationOption;
import co.elastic.otel.config.types.UrlConfigurationOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
  private final ChangesFromV1Definitions changesFromV1Definitions;

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
      final Map<String, String> validOptions,
      ChangesFromV1Definitions changesFromV1Definitions) {
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
    this.changesFromV1Definitions = changesFromV1Definitions;
  }

  public static UrlConfigurationOption.UrlConfigurationOptionBuilder urlOption() {
    return new UrlConfigurationOption.UrlConfigurationOptionBuilder();
  }

  public static BooleanConfigurationOption.BooleanConfigurationOptionBuilder booleanOption() {
    return new BooleanConfigurationOption.BooleanConfigurationOptionBuilder();
  }

  public static StringConfigurationOption.StringConfigurationOptionBuilder stringOption() {
    return new StringConfigurationOption.StringConfigurationOptionBuilder();
  }

  public static <U extends Enum<U>>
      EnumConfigurationOption.EnumConfigurationOptionBuilder<U> enumOption(Class<U> enumClass) {
    return new EnumConfigurationOption.EnumConfigurationOptionBuilder<>(enumClass);
  }

  public static ConfigurationOptionBuilder unspecifiedOption() {
    return new ConfigurationOptionBuilder();
  }

  public boolean isImplemented() {
    return description != null;
  }

  public String getKey() {
    return key;
  }

  public boolean reconcilesTo(ConfigurationOption<?> option) {
    if (isImplemented()) {
      return key.equals(option.key)
              && Objects.equals(configurationCategory, option.configurationCategory)
              && Objects.equals(label, option.label)
              && (changesFromV1Definitions.oldDescription() == null
                  ? description.equals(option.description)
                  : changesFromV1Definitions.oldDescription().equals(option.description))
              && (changesFromV1Definitions.dynamicDiff() || dynamic == option.dynamic)
              && (defaultValue instanceof Enum)
          ? Objects.equals(defaultValue.toString(), option.defaultValue.toString())
          : Objects.equals(defaultValue, option.defaultValue);
    }
    return key.equals(option.key);
  }

  public T convert(String s) throws IllegalArgumentException {
    return null;
  }

  public String toString(T value) {
    return (value == null) ? null : value.toString();
  }

  public T getCurrentValue() {
    if (value == null) {
      return defaultValue;
    } else {
      return value;
    }
  }

  public static class ConfigurationOptionBuilder<T> {
    protected String key;
    protected String category;
    protected String description;
    protected boolean dynamic;
    protected String[] tags;
    protected boolean required;
    protected T defaultValue;
    protected boolean sensitive;
    protected String label;
    protected String configurationCategory;
    protected ValueConverter<T> valueConverter;
    protected Class<? super T> valueType;
    protected List<ChangeListener<T>> changeListeners = new ArrayList<ChangeListener<T>>();
    protected List<Validator<T>> validators = new ArrayList<Validator<T>>();
    protected String[] aliasKeys = new String[0];
    protected Map<String, String> validOptions;
    protected ChangesFromV1Definitions changesFromV1Definitions = new ChangesFromV1Definitions();

    public ConfigurationOptionBuilder<T> key(String key) {
      this.key = key;
      return this;
    }

    public ConfigurationOptionBuilder<T> configurationCategory(String category) {
      this.configurationCategory = category;
      return this;
    }

    public ConfigurationOptionBuilder<T> label(String label) {
      this.label = label;
      return this;
    }

    public ConfigurationOptionBuilder<T> description(String description) {
      this.description = description;
      return this;
    }

    public ConfigurationOptionBuilder<T> dynamic(boolean dynamic) {
      this.dynamic = dynamic;
      return this;
    }

    public ConfigurationOptionBuilder<T> sensitive() {
      this.sensitive = true;
      return this;
    }

    public ConfigurationOptionBuilder<T> tags(String... tags) {
      this.tags = tags;
      return this;
    }

    public ConfigurationOption<T> buildWithDefault(T defaultValue) {
      this.required = true;
      this.defaultValue = defaultValue;
      return build();
    }

    public ConfigurationOptionBuilder<T> noLongerDynamic() {
      this.dynamic = false;
      this.changesFromV1Definitions.noLongerDynamic();
      return this;
    }

    public ConfigurationOptionBuilder<T> descriptionChange(String s1, String s2) {
      this.changesFromV1Definitions.descriptionChange(s1, s2, description);
      return this;
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
          tags == null ? new ArrayList<String>() : Arrays.asList(tags),
          required,
          changeListeners,
          validators,
          aliasKeys == null ? new ArrayList<String>() : Arrays.asList(aliasKeys),
          validOptions,
          this.changesFromV1Definitions);
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
          null,
          null);
    }
  }

  public static class ChangesFromV1Definitions {
    private boolean noLongerDynamic;
    private String removedDescription;
    private String addedDescription;
    private String description;

    public void noLongerDynamic() {
      this.noLongerDynamic = true;
    }

    public boolean dynamicDiff() {
      return this.noLongerDynamic;
    }

    public void descriptionChange(String s1, String s2, String description) {
      this.description = description;
      this.removedDescription = s1;
      this.addedDescription = s2;
    }

    public String oldDescription() {
      return description == null
          ? null
          : description.replace(this.addedDescription, this.removedDescription);
    }
  }

  public interface ValueConverter<T> {}

  public interface Validator<T> {}

  public interface ChangeListener<T> {}

  public interface ConfigurationSource {}
}
