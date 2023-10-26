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
package com.example.javaagent.smoketest;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.semconv.ResourceAttributes;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.assertj.core.api.MapAssert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.MountableFile;

public class AwsResourceProvidersTest extends TestAppSmokeTest {

  protected static final String MOCK_SERVER_HOST = "mock-server";

  private static GenericContainer<?> mockServer;

  private static MockServerClient mockServerClient;

  @BeforeAll
  public static void beforeAll() {
    mockServer = startMockServer(container -> {
      // adds an extra network name for aws k8s endpoint
      container.withNetworkAliases("kubernetes.default.svc");
    });
    mockServerClient = new MockServerClient("localhost",
        mockServer.getMappedPort(MOCK_SERVER_PORT));
  }

  @AfterAll
  public static void afterAll() {
    if (mockServer != null) {
      mockServer.stop();
    }
    if (mockServerClient != null) {
      mockServerClient.close();
    }
  }

  @AfterEach
  public void after() {
    stopApp();

    mockServerClient.reset();
  }

  @Test
  void ec2() {

    mockEc2Metadata();

    startApp(
        container -> {
          String jvmOptions = container.getEnvMap().get("JAVA_TOOL_OPTIONS");
          if (jvmOptions == null) {
            jvmOptions = "";
          }
          jvmOptions += String.format(
              " -Dotel.aws.imds.endpointOverride=%s:%d",
              MOCK_SERVER_HOST,
              MOCK_SERVER_PORT);
          container.withEnv("JAVA_TOOL_OPTIONS", jvmOptions);
        });

    testResourceProvider(attributes -> attributes
        .containsEntry(ResourceAttributes.CONTAINER_ID.getKey(),
            attributeValue(getContainerId()))
        .containsEntry(ResourceAttributes.CLOUD_PLATFORM.getKey(),
            attributeValue(ResourceAttributes.CloudPlatformValues.AWS_EC2))
        .containsEntry(ResourceAttributes.CLOUD_AVAILABILITY_ZONE.getKey(),
            attributeValue("us-west-2b")));
  }

  @Test
  void beanstalk() throws IOException {

    Path tempFile = Files.createTempFile("test", "beanstalk");
    try {
      Files.writeString(tempFile, """
          {
          "noise": "noise",
          "deployment_id":4,
          "version_label":"2",
          "environment_name":"HttpSubscriber-env"
          }
          """);
      startApp(container -> container
          .withCopyFileToContainer(
              MountableFile.forHostPath(tempFile),
              "/var/elasticbeanstalk/xray/environment.conf"));

      testResourceProvider(attributes -> attributes
          .containsEntry(ResourceAttributes.CLOUD_PLATFORM.getKey(),
              attributeValue(ResourceAttributes.CloudPlatformValues.AWS_ELASTIC_BEANSTALK))
          .containsEntry(ResourceAttributes.SERVICE_VERSION.getKey(),
              attributeValue("2"))
      );
    } finally {
      Files.delete(tempFile);
    }
  }

  @Test
  @Disabled // disabled for now due to TLS certificate setup requiring extra work
  void eks() throws IOException {
    Path tokenFile = Files.createTempFile("test", "k8sToken");
    Path certFile = Files.createTempFile("test", "k8sCert");
    try {
      Files.writeString(tokenFile, "token123");
      Files.writeString(certFile, "truststore123"); // TODO: need to replace this with a real trust store

      startApp(container -> container
          .withCopyFileToContainer(MountableFile.forHostPath(tokenFile),
              "/var/run/secrets/kubernetes.io/serviceaccount/token")
          .withCopyFileToContainer(MountableFile.forHostPath(certFile),
              "/var/run/secrets/kubernetes.io/serviceaccount/ca.crt")
      );

      testResourceProvider(attributes -> attributes
          .containsEntry(ResourceAttributes.CLOUD_PLATFORM.getKey(),
              attributeValue(ResourceAttributes.CloudPlatformValues.AWS_EKS))
          .containsEntry(ResourceAttributes.K8S_CLUSTER_NAME.getKey(),
              attributeValue("2")));

    } finally {
      Files.delete(tokenFile);
      Files.delete(certFile);
    }

  }

  @Test
  void lambda() {
    throw new RuntimeException("TODO");
  }

  @Test
  void ecs() {
    throw new RuntimeException("TODO");
  }

  private void testResourceProvider(ResourceAttributesCheck check) {
    doRequest(getUrl("/health"), okResponseBody("Alive!"));

    List<ExportTraceServiceRequest> traces = waitForTraces();
    traces.stream()
        .flatMap(t -> t.getResourceSpansList().stream())
        .map(ResourceSpans::getResource)
        .forEach(resource -> check.verify(assertThat(getAttributes(resource.getAttributesList()))));
  }

  private interface ResourceAttributesCheck {
    void verify(MapAssert<String, AnyValue> attributes);
  }

  private static void mockEc2Metadata() {
    mockServerClient.when(
        HttpRequest.request()
            .withMethod("PUT")
            .withPath("/latest/api/token")
    ).respond(HttpResponse.response().withBody("token-1234"));

    mockServerClient.when(
        HttpRequest.request()
            .withMethod("GET")
            .withPath("/latest/dynamic/instance-identity/document")
    ).respond(HttpResponse.response().withBody("""
        {
          "devpayProductCodes" : null,
          "marketplaceProductCodes" : [ "1abc2defghijklm3nopqrs4tu" ],
          "availabilityZone" : "us-west-2b",
          "privateIp" : "10.158.112.84",
          "version" : "2017-09-30",
          "instanceId" : "i-1234567890abcdef0",
          "billingProducts" : null,
          "instanceType" : "t2.micro",
          "accountId" : "123456789012",
          "imageId" : "ami-5fb8c835",
          "pendingTime" : "2016-11-19T16:32:11Z",
          "architecture" : "x86_64",
          "kernelId" : null,
          "ramdiskId" : null,
          "region" : "us-west-2"
        }
        """));

    mockServerClient.when(
        HttpRequest.request()
            .withMethod("GET")
            .withPath("/latest/meta-data/hostname")
    ).respond(HttpResponse.response().withBody("ec2-hostname"));
  }
}
