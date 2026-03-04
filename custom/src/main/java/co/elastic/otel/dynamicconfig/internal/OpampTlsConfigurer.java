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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import okhttp3.OkHttpClient;

final class OpampTlsConfigurer {
  private static final String HTTPS_SCHEME = "https://";

  private OpampTlsConfigurer() {}

  static void configure(
      OkHttpClient.Builder okHttpClient,
      String endpointUrl,
      @Nullable String certificatePath,
      @Nullable String clientKeyPath,
      @Nullable String clientCertificatePath) {
    if (certificatePath == null && clientKeyPath == null && clientCertificatePath == null) {
      return;
    }
    if (!endpointUrl.startsWith(HTTPS_SCHEME)) {
      throw new IllegalArgumentException(
          "OpAMP TLS configuration is only supported for https endpoints: " + endpointUrl);
    }
    if ((clientKeyPath == null) != (clientCertificatePath == null)) {
      throw new IllegalArgumentException(
          "Both elastic.otel.opamp.client.key and elastic.otel.opamp.client.certificate must be set");
    }
    X509TrustManager trustManager =
        certificatePath == null ? defaultTrustManager() : trustManagerFromPath(certificatePath);
    KeyManager[] keyManagers =
        clientKeyPath == null ? null : keyManagersFromPaths(clientKeyPath, clientCertificatePath);
    SSLContext sslContext = sslContext(keyManagers, trustManager);
    okHttpClient.sslSocketFactory(sslContext.getSocketFactory(), trustManager);
  }

  private static SSLContext sslContext(
      @Nullable KeyManager[] keyManagers, X509TrustManager trustManager) {
    try {
      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(keyManagers, new TrustManager[] {trustManager}, new SecureRandom());
      return sslContext;
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Unable to configure OpAMP TLS context", e);
    }
  }

  private static X509TrustManager trustManagerFromPath(String certificatePath) {
    List<X509Certificate> certificates = readCertificates(certificatePath);
    try {
      KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
      trustStore.load(null, null);
      for (int i = 0; i < certificates.size(); i++) {
        trustStore.setCertificateEntry("cert-" + i, certificates.get(i));
      }
      TrustManagerFactory trustManagerFactory =
          TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      trustManagerFactory.init(trustStore);
      return firstX509TrustManager(trustManagerFactory.getTrustManagers());
    } catch (GeneralSecurityException | IOException e) {
      throw new IllegalArgumentException(
          "Unable to load OpAMP certificate from " + certificatePath, e);
    }
  }

  private static X509TrustManager defaultTrustManager() {
    try {
      TrustManagerFactory trustManagerFactory =
          TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      trustManagerFactory.init((KeyStore) null);
      return firstX509TrustManager(trustManagerFactory.getTrustManagers());
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Unable to load default trust manager", e);
    }
  }

  private static X509TrustManager firstX509TrustManager(TrustManager[] trustManagers) {
    for (TrustManager trustManager : trustManagers) {
      if (trustManager instanceof X509TrustManager) {
        return (X509TrustManager) trustManager;
      }
    }
    throw new IllegalStateException("No X509TrustManager available for OpAMP TLS");
  }

  private static KeyManager[] keyManagersFromPaths(
      String clientKeyPath, String clientCertificatePath) {
    PrivateKey privateKey = readPrivateKey(clientKeyPath);
    List<X509Certificate> certificates = readCertificates(clientCertificatePath);
    try {
      KeyStore keyStore = KeyStore.getInstance("PKCS12");
      keyStore.load(null, null);
      char[] password = new char[0];
      keyStore.setKeyEntry(
          "opamp-client", privateKey, password, certificates.toArray(new Certificate[0]));
      KeyManagerFactory keyManagerFactory =
          KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      keyManagerFactory.init(keyStore, password);
      return keyManagerFactory.getKeyManagers();
    } catch (GeneralSecurityException | IOException e) {
      throw new IllegalArgumentException("Unable to load OpAMP client certificate or key", e);
    }
  }

  private static List<X509Certificate> readCertificates(String certificatePath) {
    try {
      byte[] certificateBytes = Files.readAllBytes(Paths.get(certificatePath));
      CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
      Collection<? extends Certificate> parsed =
          certificateFactory.generateCertificates(new ByteArrayInputStream(certificateBytes));
      if (parsed.isEmpty()) {
        throw new IllegalArgumentException(
            "No certificates found in OpAMP certificate file: " + certificatePath);
      }
      List<X509Certificate> certificates = new ArrayList<>(parsed.size());
      for (Certificate certificate : parsed) {
        certificates.add((X509Certificate) certificate);
      }
      return certificates;
    } catch (IOException | GeneralSecurityException e) {
      throw new IllegalArgumentException(
          "Unable to read OpAMP certificate file: " + certificatePath, e);
    }
  }

  private static PrivateKey readPrivateKey(String clientKeyPath) {
    PemBlock pemBlock = PemBlock.fromFile(clientKeyPath);
    byte[] derBytes = Base64.getMimeDecoder().decode(pemBlock.base64);
    switch (pemBlock.type) {
      case "PRIVATE KEY":
        return decodePkcs8(derBytes, clientKeyPath);
      case "RSA PRIVATE KEY":
        return decodePkcs8(wrapRsaPkcs1ToPkcs8(derBytes), clientKeyPath);
      default:
        throw new IllegalArgumentException(
            "Unsupported private key type in " + clientKeyPath + ": " + pemBlock.type);
    }
  }

  private static PrivateKey decodePkcs8(byte[] pkcs8Bytes, String clientKeyPath) {
    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8Bytes);
    for (String algorithm : new String[] {"RSA", "EC", "DSA"}) {
      try {
        return KeyFactory.getInstance(algorithm).generatePrivate(keySpec);
      } catch (GeneralSecurityException ignored) {
        // try next algorithm
      }
    }
    throw new IllegalArgumentException("Unable to parse OpAMP private key: " + clientKeyPath);
  }

  private static byte[] wrapRsaPkcs1ToPkcs8(byte[] pkcs1Bytes) {
    byte[] rsaAlgorithmIdentifier =
        new byte[] {
          0x30,
          0x0d,
          0x06,
          0x09,
          0x2a,
          (byte) 0x86,
          0x48,
          (byte) 0x86,
          (byte) 0xf7,
          0x0d,
          0x01,
          0x01,
          0x01,
          0x05,
          0x00
        };
    byte[] privateKeyOctetString = derOctetString(pkcs1Bytes);
    byte[] privateKeyInfo =
        derSequence(
            concat(derInteger(new byte[] {0x00}), rsaAlgorithmIdentifier, privateKeyOctetString));
    return privateKeyInfo;
  }

  private static byte[] derSequence(byte[] content) {
    return concat(new byte[] {0x30}, derLength(content.length), content);
  }

  private static byte[] derInteger(byte[] value) {
    return concat(new byte[] {0x02}, derLength(value.length), value);
  }

  private static byte[] derOctetString(byte[] value) {
    return concat(new byte[] {0x04}, derLength(value.length), value);
  }

  private static byte[] derLength(int length) {
    if (length < 0x80) {
      return new byte[] {(byte) length};
    }
    int numBytes = 0;
    int temp = length;
    while (temp > 0) {
      temp >>= 8;
      numBytes++;
    }
    byte[] result = new byte[1 + numBytes];
    result[0] = (byte) (0x80 | numBytes);
    for (int i = numBytes; i > 0; i--) {
      result[i] = (byte) (length & 0xff);
      length >>= 8;
    }
    return result;
  }

  private static byte[] concat(byte[]... parts) {
    int length = 0;
    for (byte[] part : parts) {
      length += part.length;
    }
    byte[] result = new byte[length];
    int offset = 0;
    for (byte[] part : parts) {
      System.arraycopy(part, 0, result, offset, part.length);
      offset += part.length;
    }
    return result;
  }

  private static final class PemBlock {
    private final String type;
    private final String base64;

    private PemBlock(String type, String base64) {
      this.type = type;
      this.base64 = base64;
    }

    private static PemBlock fromFile(String path) {
      String contents;
      try {
        contents = new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.US_ASCII);
      } catch (IOException e) {
        throw new IllegalArgumentException("Unable to read OpAMP private key: " + path, e);
      }
      int beginIndex = contents.indexOf("-----BEGIN ");
      int endIndex = contents.indexOf("-----END ");
      if (beginIndex < 0 || endIndex < 0) {
        throw new IllegalArgumentException("Unable to parse OpAMP private key: " + path);
      }
      int typeStart = beginIndex + "-----BEGIN ".length();
      int typeEnd = contents.indexOf("-----", typeStart);
      if (typeEnd < 0) {
        throw new IllegalArgumentException("Unable to parse OpAMP private key: " + path);
      }
      String type = contents.substring(typeStart, typeEnd).trim();
      int base64Start = contents.indexOf('\n', typeEnd);
      int base64End = contents.indexOf("-----END", base64Start);
      if (base64Start < 0 || base64End < 0) {
        throw new IllegalArgumentException("Unable to parse OpAMP private key: " + path);
      }
      String base64 =
          contents.substring(base64Start, base64End).replace("\r", "").replace("\n", "").trim();
      if (base64.isEmpty()) {
        throw new IllegalArgumentException("Unable to parse OpAMP private key: " + path);
      }
      return new PemBlock(type, base64);
    }
  }
}
