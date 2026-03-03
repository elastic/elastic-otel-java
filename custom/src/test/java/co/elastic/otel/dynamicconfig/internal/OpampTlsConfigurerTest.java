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
package co.elastic.otel.dynamicconfig.internal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OpampTlsConfigurerTest {

  @TempDir Path tempDir;

  @Test
  void configure_noTlsValuesIsNoop() {
    OkHttpClient.Builder builder = new OkHttpClient().newBuilder();
    assertThatCode(
            () ->
                OpampTlsConfigurer.configure(
                    builder, "http://localhost:4320/v1/opamp", null, null, null))
        .doesNotThrowAnyException();
  }

  @Test
  void configure_throwsForNonHttpsEndpointWhenTlsConfigured() {
    OkHttpClient.Builder builder = new OkHttpClient().newBuilder();
    assertThatThrownBy(
            () ->
                OpampTlsConfigurer.configure(
                    builder, "http://localhost:4320/v1/opamp", "ca.pem", null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("https endpoints");
  }

  @Test
  void configure_throwsWhenClientKeyOrCertificateMissing() {
    OkHttpClient.Builder builder = new OkHttpClient().newBuilder();
    assertThatThrownBy(
            () ->
                OpampTlsConfigurer.configure(
                    builder, "https://localhost:4320/v1/opamp", null, "client-key.pem", null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("client.key")
        .hasMessageContaining("client.certificate");
  }

  @Test
  void configure_throwsWhenCertificateFileIsInvalid() throws IOException {
    Path invalidCertificate = tempDir.resolve("invalid-ca.pem");
    Files.write(invalidCertificate, "not a certificate".getBytes(StandardCharsets.US_ASCII));

    OkHttpClient.Builder builder = new OkHttpClient().newBuilder();
    assertThatThrownBy(
            () ->
                OpampTlsConfigurer.configure(
                    builder,
                    "https://localhost:4320/v1/opamp",
                    invalidCertificate.toString(),
                    null,
                    null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unable to read OpAMP certificate file");
  }

  @Test
  void configure_throwsWhenPrivateKeyFileDoesNotExist() {
    OkHttpClient.Builder builder = new OkHttpClient().newBuilder();
    assertThatThrownBy(
            () ->
                OpampTlsConfigurer.configure(
                    builder,
                    "https://localhost:4320/v1/opamp",
                    null,
                    tempDir.resolve("missing-client-key.pem").toString(),
                    tempDir.resolve("client-cert.pem").toString()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unable to read OpAMP private key");
  }
}
