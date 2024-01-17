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

import co.elastic.otel.common.util.Version;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ReconcileOptionsTest {
  private static final Pattern VERSION_EXTRACTION_REGEX =
      Pattern.compile("^\\s*<[aA]\\s+[hH][rR][eE][fF]=\"(\\d+\\.\\d+\\.\\d+)/\"");
  public static final String USER_AGENT = "elastic-otel-java-option-reconciler";
  public static final String MAVEN_ROOT_URL =
      "https://repo1.maven.org/maven2/co/elastic/apm/elastic-apm-agent/";

  private static File V1ElasticAgentJarLocation;
  private static List<ConfigurationOption> AllOptionsFromV1ElasticAgentJar;

  @BeforeAll
  public static void getLatestV1ElasticAgentJarAndOptions() throws Exception {
    V1ElasticAgentJarLocation = downloadLatestV1ElasticAgentJar();
    AllOptionsFromV1ElasticAgentJar = getAllOptionsFromV1ElasticAgentJar(V1ElasticAgentJarLocation);
  }

  @AfterAll
  public static void cleanUp() {
    V1ElasticAgentJarLocation.delete();
  }

  @Test
  public void reconcileAllOptions() throws Exception {
    Configurations inst = new Configurations();
    List<ConfigurationOption> notInThisAgent = inV1ButNot(inst.getAllOptions());
    assertThat(notInThisAgent).isEmpty();
  }

  @Test
  public void reconcileWithoutOneOption() throws Exception {
    Configurations inst = new Configurations();
    List<ConfigurationOption> allOptionsLessOne = inst.getAllOptions();
    allOptionsLessOne.remove(allOptionsLessOne.size() - 1);
    List<ConfigurationOption> notInThisAgent = inV1ButNot(allOptionsLessOne);
    assertThat(notInThisAgent).hasSize(1);
  }

  @Test
  public void reconcileWithOneDifferentOption() throws Exception {
    Configurations inst = new Configurations();
    List<ConfigurationOption> allOptionsWithOneDifferent = inst.getAllOptions();
    allOptionsWithOneDifferent.remove(allOptionsWithOneDifferent.size() - 1);
    allOptionsWithOneDifferent.add(
        new ConfigurationOption("this_is_never_going_to_be_a_real_option_cvui", null));
    List<ConfigurationOption> notInThisAgent = inV1ButNot(allOptionsWithOneDifferent);
    assertThat(notInThisAgent).hasSize(1);
  }

  private List<ConfigurationOption> inV1ButNot(List<ConfigurationOption> theseOptions)
      throws Exception {
    Map<String, ConfigurationOption> allThisAgentsOptionsByKey = new HashMap<>();
    for (ConfigurationOption option : theseOptions) {
      allThisAgentsOptionsByKey.put(option.getKey(), option);
    }
    List<ConfigurationOption> notInThisAgent = new ArrayList<>();
    for (ConfigurationOption v1Option : AllOptionsFromV1ElasticAgentJar) {
      if (allThisAgentsOptionsByKey.containsKey(v1Option.getKey())) {
        ConfigurationOption myOption = allThisAgentsOptionsByKey.get(v1Option.getKey());
        if (myOption.isImplemented() && !myOption.reconcilesTo(v1Option)) {
          notInThisAgent.add(v1Option);
        }
      } else {
        notInThisAgent.add(v1Option);
      }
    }
    return notInThisAgent;
  }

  private static List<ConfigurationOption> getAllOptionsFromV1ElasticAgentJar(
      File v1ElasticAgentJarLocation) throws Exception {
    ShadedClassLoader cl =
        new ShadedClassLoader(
            v1ElasticAgentJarLocation, ReconcileOptionsTest.class.getClassLoader(), "agent/");
    Class<?> configurationOptionProviderClazz =
        cl.loadClass("org.stagemonitor.configuration.ConfigurationOptionProvider");
    Iterable<?> optionProvidersList = ServiceLoader.load(configurationOptionProviderClazz, cl);
    Class<?> configurationRegistryClazz =
        cl.loadClass("org.stagemonitor.configuration.ConfigurationRegistry");
    Object builder = execute(configurationRegistryClazz, null, "builder", new Object[0]);
    Object optionProviders =
        execute(builder.getClass(), builder, "optionProviders", new Object[] {optionProvidersList});
    Object configurationRegistry =
        execute(optionProviders.getClass(), optionProviders, "build", new Object[0]);
    Map<?, ?> configurationOptionsByCategory =
        (Map<?, ?>)
            execute(
                configurationRegistry.getClass(),
                configurationRegistry,
                "getConfigurationOptionsByCategory",
                new Object[0]);
    List<ConfigurationOption> allOptions = new ArrayList<>();
    for (Object object : configurationOptionsByCategory.values()) {
      for (Object object2 : (Iterable<?>) object) {
        allOptions.add(configurationOptionFrom(object2));
      }
    }

    return allOptions;
  }

  private static ConfigurationOption configurationOptionFrom(Object obj)
      throws NoSuchFieldException, IllegalAccessException {
    Class<?> clazz = obj.getClass();
    boolean dynamic = (boolean) getField(clazz, obj, "dynamic");
    boolean sensitive = (boolean) getField(clazz, obj, "sensitive");
    String key = (String) getField(clazz, obj, "key");
    List<String> aliasKeys = (List<String>) getField(clazz, obj, "aliasKeys");
    List<String> allKeys = (List<String>) getField(clazz, obj, "allKeys");
    String label = (String) getField(clazz, obj, "label");
    String description = (String) getField(clazz, obj, "description");
    Object defaultValue = getField(clazz, obj, "defaultValue");
    List<String> tags = (List<String>) getField(clazz, obj, "tags");
    //    List<ConfigurationOption.Validator> validators = null;
    //    List<ConfigurationOption.ChangeListener> changeListeners = null;
    boolean required = (boolean) getField(clazz, obj, "required");
    String defaultValueAsString = (String) getField(clazz, obj, "defaultValueAsString");
    String configurationCategory = (String) getField(clazz, obj, "configurationCategory");
    //    ConfigurationOption.ValueConverter valueConverter = null;
    Class valueType = (Class) getField(clazz, obj, "valueType");
    Map<String, String> validOptions = (Map<String, String>) getField(clazz, obj, "validOptions");
    String valueAsString = (String) getField(clazz, obj, "valueAsString");
    Object value = getField(clazz, obj, "value");
    //    configurationSources = null;
    String nameOfCurrentConfigurationSource =
        (String) getField(clazz, obj, "nameOfCurrentConfigurationSource");
    String errorMessage = (String) getField(clazz, obj, "errorMessage");
    String usedKey = (String) getField(clazz, obj, "usedKey");
    return new ConfigurationOption(key, description);
  }

  private static Object getField(Class<?> clazz, Object obj, String fieldName)
      throws NoSuchFieldException,
          SecurityException,
          IllegalArgumentException,
          IllegalAccessException {
    Class<?>[] parameterTypes = new Class[0];
    Field field = clazz.getDeclaredField(fieldName);
    field.setAccessible(true);
    return field.get(obj);
  }

  private static Object execute(Class<?> clazz, Object obj, String methodName, Object[] args)
      throws NoSuchMethodException,
          SecurityException,
          IllegalAccessException,
          IllegalArgumentException,
          InvocationTargetException {
    Class<?>[] parameterTypes = new Class[args.length];
    for (int i = 0; i < parameterTypes.length; i++) {
      Class<? extends Object> clazz2 = args[i].getClass();
      if (clazz2 == java.util.ServiceLoader.class) {
        clazz2 = Iterable.class;
      }
      parameterTypes[i] = clazz2;
    }
    Method method = clazz.getMethod(methodName, parameterTypes);
    return method.invoke(obj, args);
  }

  private static File downloadLatestV1ElasticAgentJar() throws Exception {
    String latest = getLatestV1ElasticAgentVersion().toString();
    File localFile = File.createTempFile("elastic-apm-agent-" + latest, ".jar");
    localFile.deleteOnExit();
    try (HttpURLConnectionWrapper jarConnection =
        HttpURLConnectionWrapper.openConnection(
            MAVEN_ROOT_URL + latest + "/elastic-apm-agent-" + latest + ".jar")) {
      InputStream in = jarConnection.getInputStream();
      Files.copy(in, localFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
    return localFile;
  }

  private static Version getLatestV1ElasticAgentVersion() throws Exception {
    try (HttpURLConnectionWrapper httpURLConnection =
        HttpURLConnectionWrapper.openConnection(MAVEN_ROOT_URL)) {
      BufferedReader versionsHtmlReader =
          new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
      TreeSet<Version> versions = new TreeSet<>();
      String line;
      while ((line = versionsHtmlReader.readLine()) != null) {
        try {
          Matcher matcher = VERSION_EXTRACTION_REGEX.matcher(line);
          if (matcher.find()) {
            Version version = Version.of(matcher.group(1));
            if (!version.hasSuffix()) {
              versions.add(version);
            }
          }
        } catch (Exception e) {
          // ignore, probably a regex false positive
        }
      }
      return versions.last();
    }
  }

  public static class HttpURLConnectionWrapper implements AutoCloseable {

    private final HttpURLConnection httpURLConnection;
    private InputStream inputStream;

    public HttpURLConnectionWrapper(HttpURLConnection httpURLConnection) {
      this.httpURLConnection = httpURLConnection;
    }

    @Override
    public void close() throws Exception {
      if (this.inputStream != null) {
        this.inputStream.close();
      }
      if (this.httpURLConnection != null) {
        this.httpURLConnection.disconnect();
      }
    }

    private static HttpURLConnectionWrapper openConnection(String urlString) throws IOException {
      URL url = new URL(urlString);
      HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
      urlConnection.addRequestProperty("User-Agent", USER_AGENT);
      return new HttpURLConnectionWrapper(urlConnection);
    }

    public InputStream getInputStream() throws IOException {
      if (this.inputStream == null) {
        this.inputStream = this.httpURLConnection.getInputStream();
      }
      return this.inputStream;
    }
  }

  /** Copied from Elastic Agent https://github.com/elastic/apm-agent-java and stripped a little */
  public static class ShadedClassLoader extends URLClassLoader {

    public static final String SHADED_CLASS_EXTENSION = ".esclazz";
    private static final String CLASS_EXTENSION = ".class";

    private final String customPrefix;
    private final Manifest manifest;
    private final URL jarUrl;
    private final ThreadLocal<Set<String>> locallyNonAvailableResources =
        new ThreadLocal<Set<String>>() {
          @Override
          protected Set<String> initialValue() {
            return new HashSet<>();
          }
        };

    public ShadedClassLoader(File jar, ClassLoader parent, String customPrefix) throws IOException {
      super(new URL[] {jar.toURI().toURL()}, parent);
      this.customPrefix = customPrefix;
      this.jarUrl = jar.toURI().toURL();
      try (JarFile jarFile = new JarFile(jar, false)) {
        this.manifest = jarFile.getManifest();
      }
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      synchronized (getClassLoadingLock(name)) {
        try {
          // First, check if the class has already been loaded
          Class<?> c = findLoadedClass(name);
          if (c == null) {
            c = findClass(name);
            if (resolve) {
              resolveClass(c);
            }
          }
          return c;
        } catch (ClassNotFoundException e) {
          return super.loadClass(name, resolve);
        }
      }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
      byte[] classBytes = getShadedClassBytes(name);
      if (classBytes != null) {
        return defineClass(name, classBytes);
      }
      throw new ClassNotFoundException(name);
    }

    private Class<?> defineClass(String name, byte[] classBytes) {
      String packageName = getPackageName(name);
      if (packageName != null && !isPackageDefined(packageName)) {
        try {
          if (manifest != null) {
            definePackage(packageName, manifest, jarUrl);
          } else {
            definePackage(packageName, null, null, null, null, null, null, null);
          }
        } catch (IllegalArgumentException e) {
          // The package may have been defined by a parent class loader in the meantime
          if (!isPackageDefined(packageName)) {
            throw e;
          }
        }
      }

      return defineClass(name, classBytes, 0, classBytes.length);
    }

    @SuppressWarnings("deprecation")
    private boolean isPackageDefined(String packageName) {
      // The 'getPackage' method is deprecated as of Java 9, 'getDefinedPackage' is the alternative.
      //
      // The only difference is that 'getDefinedPackage' does not delegate to parent CL for lookup.
      // Given we are only interested on the fact that the package is defined or not without caring
      // about which CL
      // has defined it, it does not make any difference in our case.
      return getPackage(packageName) != null;
    }

    public String getPackageName(String className) {
      int i = className.lastIndexOf('.');
      if (i != -1) {
        return className.substring(0, i);
      }
      return null;
    }

    private byte[] getShadedClassBytes(String name) throws ClassNotFoundException {
      try (InputStream is =
          getResourceAsStreamInternal(
              customPrefix + name.replace('.', '/') + SHADED_CLASS_EXTENSION)) {
        if (is != null) {
          ByteArrayOutputStream buffer = new ByteArrayOutputStream();
          int n;
          byte[] data = new byte[1024];
          while ((n = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, n);
          }
          return buffer.toByteArray();
        }
      } catch (IOException e) {
        throw new ClassNotFoundException(name, e);
      }
      return null;
    }

    private InputStream getResourceAsStreamInternal(String name) {
      return super.getResourceAsStream(name);
    }

    @Override
    public URL findResource(final String name) {
      if (locallyNonAvailableResources.get().contains(name)) {
        return null;
      }

      return findResourceInternal(getShadedResourceName(name));
    }

    private URL findResourceInternal(String name) {
      return super.findResource(name);
    }

    @Override
    public Enumeration<URL> findResources(final String name) throws IOException {
      if (locallyNonAvailableResources.get().contains(name)) {
        return Collections.emptyEnumeration();
      }

      Enumeration<URL> result = super.findResources(getShadedResourceName(name));
      return result;
    }

    @Override
    public URL getResource(String name) {
      // look locally first
      URL shadedResource = findResource(name);
      if (shadedResource != null) {
        return shadedResource;
      }
      // if not found locally, calling super's lookup, which does parent first and then local, so
      // marking as not required for local lookup
      Set<String> locallyNonAvailableResources = this.locallyNonAvailableResources.get();
      try {
        locallyNonAvailableResources.add(name);
        return super.getResource(name);
      } finally {
        locallyNonAvailableResources.remove(name);
      }
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
      // look locally first
      Enumeration<URL> shadedResources = findResources(name);
      if (shadedResources.hasMoreElements()) {
        // no need to compound results with parent lookup, we only want to return the shaded form if
        // there is such
        return shadedResources;
      }
      // if not found locally, calling super's lookup, which does parent first and then local, so
      // marking as not required for local lookup
      Set<String> locallyNonAvailableResources = this.locallyNonAvailableResources.get();
      try {
        locallyNonAvailableResources.add(name);
        return super.getResources(name);
      } finally {
        locallyNonAvailableResources.remove(name);
      }
    }

    private String getShadedResourceName(String name) {
      if (name.startsWith(customPrefix)) {
        // already a lookup of the shaded form
        return name;
      } else if (name.endsWith(CLASS_EXTENSION)) {
        return customPrefix
            + name.substring(0, name.length() - CLASS_EXTENSION.length())
            + SHADED_CLASS_EXTENSION;
      } else {
        return customPrefix + name;
      }
    }

    @Override
    public String toString() {
      return "ShadedClassLoader{"
          + "parent="
          + getParent()
          + ", customPrefix='"
          + customPrefix
          + '\''
          + ", manifest="
          + manifest
          + ", jarUrl="
          + jarUrl
          + '}';
    }
  }
}
