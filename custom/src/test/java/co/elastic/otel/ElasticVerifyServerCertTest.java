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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ElasticVerifyServerCertTest {

  private static final String DUMMY_KEYSTORE_PATH;
  public static final String DUMMY_KEYSTORE_PWD = "1234";

  static {
    try {
      URL url = ElasticVerifyServerCert.class.getClassLoader().getResource("dummy.keystore");
      assertThat(url).isNotNull();
      Path path = Paths.get(url.toURI());
      assertThat(path).exists();
      DUMMY_KEYSTORE_PATH = path.toAbsolutePath().toString();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
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

  @Test
  void openKnownKeystore() throws Exception {
    KeyStore ks = getKeyStore(DUMMY_KEYSTORE_PATH, DUMMY_KEYSTORE_PWD, null, null);
    assertThat(ks).isNotNull();
    assertThat(ks.getType()).isEqualTo("pkcs12");
    assertThat(ks.getProvider().getName()).isEqualTo("SUN");
  }

  @Test
  void keyManagers_noKeyStore() throws Exception {
    Properties config = new Properties();
    assertThat(getKeyManagers(config)).isNotNull();
  }

  @Test
  void keyManagers_knownKeyStore() throws Exception {
    Properties config = new Properties();
    config.put("javax.net.ssl.keyStore", DUMMY_KEYSTORE_PATH);
    config.put("javax.net.ssl.keyStorePassword", DUMMY_KEYSTORE_PWD);
    config.put("javax.net.ssl.keyStoreType", "pkcs12");
    config.put("javax.net.ssl.keyStoreProvider", "SUN");
    assertThat(getKeyManagers(config)).isNotNull();
  }
}
