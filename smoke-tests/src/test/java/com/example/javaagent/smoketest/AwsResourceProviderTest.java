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
import static org.mockserver.model.HttpRequest.request;

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.semconv.ResourceAttributes;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.MapAssert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.testcontainers.containers.GenericContainer;

public class AwsResourceProviderTest extends TestAppSmokeTest {

  private static GenericContainer<?> mockServer;

  private static MockServerClient mockServerClient;

  @BeforeAll
  public static void beforeAll() {
    mockServer = startMockServer();
    mockServerClient = new MockServerClient("localhost",
        mockServer.getMappedPort(MOCK_SERVER_PORT));
  }

  @AfterAll
  public static void afterAll() {
    if (mockServer != null) {
      mockServer.stop();
    }
    if(mockServerClient != null) {
      mockServerClient.close();
    }
  }

  @AfterEach
  public void after() {
    stopApp();

    mockServerClient.reset();
  }

  @Test
  void getEc2Resource() {

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
          return container;
        });

    testResourceProvider(attributes -> attributes
        .containsEntry(ResourceAttributes.CONTAINER_ID.getKey(),
            attributeValue(getContainerId()))
        .containsEntry(ResourceAttributes.CLOUD_PLATFORM.getKey(),
            attributeValue(ResourceAttributes.CloudPlatformValues.AWS_EC2))
        .containsEntry(ResourceAttributes.CLOUD_AVAILABILITY_ZONE.getKey(),
            attributeValue("us-west-2b")));
  }

  private void testResourceProvider (ResourceAttributesCheck check){
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
