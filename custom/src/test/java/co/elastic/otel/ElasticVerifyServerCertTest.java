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

import static co.elastic.otel.ElasticVerifyServerCert.getKeyManagers;
import static co.elastic.otel.ElasticVerifyServerCert.getKeyStore;
import static co.elastic.otel.ElasticVerifyServerCert.verifyServerCertificate;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyStore;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ElasticVerifyServerCertTest {

  private static final String DUMMY_KEYSTORE_PWD = "1234";

  private static @TempDir Path tmp;

  private static Path createKeyStore(String type) {
    Path path;
    try {
      path = Files.createTempFile(tmp, "dummy-keystore", "." + type);
      KeyStore keyStore = KeyStore.getInstance(type);
      keyStore.load(null, null);
      try (OutputStream output =
          Files.newOutputStream(
              path,
              StandardOpenOption.WRITE,
              StandardOpenOption.TRUNCATE_EXISTING,
              StandardOpenOption.CREATE)) {
        keyStore.store(output, DUMMY_KEYSTORE_PWD.toCharArray());
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return path.toAbsolutePath();
  }

  @Test
  void verifyByDefault() {
    ConfigProperties config = DefaultConfigProperties.createFromMap(Collections.emptyMap());
    assertThat(verifyServerCertificate(config)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void configureExplicitly(boolean verify) {
    Map<String, String> map = new HashMap<>();
    map.put("elastic.otel.verify.server.cert", Boolean.toString(verify));
    ConfigProperties config = DefaultConfigProperties.createFromMap(map);
    assertThat(verifyServerCertificate(config)).isEqualTo(verify);
  }

  @ParameterizedTest
  @ValueSource(strings = {"pkcs12", "jks"})
  void openKeystore(String type) throws Exception {
    Path path = createKeyStore(type);
    KeyStore ks = getKeyStore(path, DUMMY_KEYSTORE_PWD, null, null);
    assertThat(ks).isNotNull();
    assertThat(ks.getType()).isEqualTo(KeyStore.getDefaultType());
    assertThat(ks.getProvider().getName()).isEqualTo("SUN");
  }

  @Test
  void keyManagers_noKeyStore() throws Exception {
    Properties config = new Properties();
    assertThat(getKeyManagers(config)).isNotNull();
  }

  @ParameterizedTest
  @ValueSource(strings = {"pkcs12", "jks"})
  void keyManagers_keyStore(String type) throws Exception {
    Path path = createKeyStore(type);
    Properties config = new Properties();
    config.put("javax.net.ssl.keyStore", path.toString());
    config.put("javax.net.ssl.keyStorePassword", DUMMY_KEYSTORE_PWD);
    config.put("javax.net.ssl.keyStoreType", KeyStore.getDefaultType());
    config.put("javax.net.ssl.keyStoreProvider", "SUN");
    assertThat(getKeyManagers(config)).isNotNull();
  }
}
