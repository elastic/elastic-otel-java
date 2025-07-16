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

import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Properties;
import javax.annotation.Nullable;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class ElasticVerifyServerCert {

  private static final X509TrustManager X_509_TRUST_ALL =
      new X509TrustManager() {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {}

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {}

        @Override
        public X509Certificate[] getAcceptedIssuers() {
          return new X509Certificate[0];
        }
      };

  // package protected for testing
  static boolean verifyServerCertificate(ConfigProperties config) {
    return config.getBoolean("elastic.otel.verify.server.cert", true);
  }

  public static SpanExporter configureIfPossible(
      SpanExporter spanExporter, ConfigProperties config) {
    if (verifyServerCertificate(config)) {
      return spanExporter;
    }
    if (spanExporter instanceof OtlpGrpcSpanExporter) {
      return ((OtlpGrpcSpanExporter) spanExporter)
          .toBuilder().setSslContext(getSslContext(), X_509_TRUST_ALL).build();
    } else if (spanExporter instanceof OtlpHttpSpanExporter) {
      return ((OtlpHttpSpanExporter) spanExporter)
          .toBuilder().setSslContext(getSslContext(), X_509_TRUST_ALL).build();
    }
    return spanExporter;
  }

  public static MetricExporter configureIfPossible(
      MetricExporter metricExporter, ConfigProperties config) {
    if (verifyServerCertificate(config)) {
      return metricExporter;
    }
    if (metricExporter instanceof OtlpGrpcMetricExporter) {
      return ((OtlpGrpcMetricExporter) metricExporter)
          .toBuilder().setSslContext(getSslContext(), X_509_TRUST_ALL).build();
    } else if (metricExporter instanceof OtlpHttpMetricExporter) {
      return ((OtlpHttpMetricExporter) metricExporter)
          .toBuilder().setSslContext(getSslContext(), X_509_TRUST_ALL).build();
    }
    return metricExporter;
  }

  public static LogRecordExporter configureIfPossible(
      LogRecordExporter logExporter, ConfigProperties config) {
    if (verifyServerCertificate(config)) {
      return logExporter;
    }
    if (logExporter instanceof OtlpGrpcLogRecordExporter) {
      return ((OtlpGrpcLogRecordExporter) logExporter)
          .toBuilder().setSslContext(getSslContext(), X_509_TRUST_ALL).build();
    } else if (logExporter instanceof OtlpHttpLogRecordExporter) {
      return ((OtlpHttpLogRecordExporter) logExporter)
          .toBuilder().setSslContext(getSslContext(), X_509_TRUST_ALL).build();
    }
    return logExporter;
  }

  private static SSLContext getSslContext() {
    SSLContext sslContext;
    try {
      sslContext = SSLContext.getInstance("TLS");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Unable to create SSL/TLS context", e);
    }

    KeyManager[] keyManagers = null;
    try {
      keyManagers = getKeyManagers(System.getProperties());
    } catch (IOException | GeneralSecurityException e) {
      // silently ignored
      // trust and key stores won't be available, which means client certificate can't be used
    }

    try {
      sslContext.init(
          keyManagers, new TrustManager[] {X_509_TRUST_ALL}, new java.security.SecureRandom());
    } catch (KeyManagementException e) {
      throw new IllegalStateException("unable to initialize SSL/TLS context", e);
    }

    return sslContext;
  }

  @Nullable
  static KeyManager[] getKeyManagers(Properties properties)
      throws IOException, GeneralSecurityException {
    // re-implements parts of sun.security.ssl.SSLContextImpl.DefaultManagersHolder.getKeyManagers
    // as there is no simple way to reuse existing implementation

    String path = properties.getProperty("javax.net.ssl.keyStore");
    String pwd = properties.getProperty("javax.net.ssl.keyStorePassword");
    String type = properties.getProperty("javax.net.ssl.keyStoreType");
    String provider = properties.getProperty("javax.net.ssl.keyStoreProvider");

    KeyStore ks = null;
    try {
      ks = getKeyStore(path, pwd, type, provider);
    } catch (IOException | GeneralSecurityException e) {
      // silently ignore, client certificate won't work
    }

    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    kmf.init(ks, pwd != null ? pwd.toCharArray() : null);
    return kmf.getKeyManagers();
  }

  // package private for testing
  @Nullable
  static KeyStore getKeyStore(String keyStore, @Nullable String keyStorePassword,
      @Nullable String keyStoreType, @Nullable String keyStoreProvider)
      throws IOException, GeneralSecurityException {
    String type = keyStoreType != null ? keyStoreType : KeyStore.getDefaultType();
    if (keyStore == null) {
      return null;
    }
    try (FileInputStream input = new FileInputStream(keyStore)) {
      KeyStore ks = keyStoreProvider == null
          ? KeyStore.getInstance(type)
          : KeyStore.getInstance(type, keyStoreProvider);
      ks.load(input, keyStorePassword != null ? keyStorePassword.toCharArray() : null);
      return ks;
    }
  }
}
