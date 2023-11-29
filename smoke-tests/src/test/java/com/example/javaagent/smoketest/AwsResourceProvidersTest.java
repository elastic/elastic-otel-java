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

import io.opentelemetry.semconv.ResourceAttributes;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

@Disabled // because currently flaky
public class AwsResourceProvidersTest extends TestAppSmokeTest {

  protected static final String MOCK_SERVER_HOST = "mock-server";

  private static GenericContainer<?> mockServer;

  private static MockServerClient mockServerClient;

  @BeforeAll
  public static void beforeAll() {
    mockServer =
        startMockServer(
            container -> {
              // adds an extra network name for aws k8s endpoint
              container.withNetworkAliases("kubernetes.default.svc");
            });
    mockServerClient =
        new MockServerClient("localhost", mockServer.getMappedPort(MOCK_SERVER_PORT));
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

    startTestApp(
        container -> {
          String jvmOptions = container.getEnvMap().get("JAVA_TOOL_OPTIONS");
          if (jvmOptions == null) {
            jvmOptions = "";
          }
          jvmOptions +=
              String.format(
                  " -Dotel.aws.imds.endpointOverride=%s:%d", MOCK_SERVER_HOST, MOCK_SERVER_PORT);
          container.withEnv("JAVA_TOOL_OPTIONS", jvmOptions);
        });

    testResourceProvider(
        attributes ->
            attributes
                .containsEntry(
                    ResourceAttributes.CONTAINER_ID.getKey(), attributeValue(getContainerId()))
                .containsEntry(
                    ResourceAttributes.CLOUD_PLATFORM.getKey(),
                    attributeValue(ResourceAttributes.CloudPlatformValues.AWS_EC2))
                .containsEntry(
                    ResourceAttributes.CLOUD_AVAILABILITY_ZONE.getKey(),
                    attributeValue("us-west-2b")));
  }

  @Test
  void beanstalk() throws IOException {

    Path tempFile = Files.createTempFile("test", "beanstalk");
    try {
      Files.writeString(
          tempFile,
          """
          {
          "noise": "noise",
          "deployment_id":4,
          "version_label":"2",
          "environment_name":"HttpSubscriber-env"
          }
          """);
      startTestApp(
          container ->
              container.withCopyFileToContainer(
                  MountableFile.forHostPath(tempFile),
                  "/var/elasticbeanstalk/xray/environment.conf"));

      testResourceProvider(
          attributes ->
              attributes
                  .containsEntry(
                      ResourceAttributes.CLOUD_PLATFORM.getKey(),
                      attributeValue(ResourceAttributes.CloudPlatformValues.AWS_ELASTIC_BEANSTALK))
                  .containsEntry(ResourceAttributes.SERVICE_VERSION.getKey(), attributeValue("2")));
    } finally {
      Files.delete(tempFile);
    }
  }

  @Test
  @Disabled
  // disabled for now due to TLS certificate setup requiring extra work
  void eks() throws IOException {
    Path tokenFile = Files.createTempFile("test", "k8sToken");
    Path certFile = Files.createTempFile("test", "k8sCert");
    try {
      Files.writeString(tokenFile, "token123");
      Files.writeString(
          certFile, "truststore123"); // TODO: need to replace this with a real trust store

      startTestApp(
          container ->
              container
                  .withCopyFileToContainer(
                      MountableFile.forHostPath(tokenFile),
                      "/var/run/secrets/kubernetes.io/serviceaccount/token")
                  .withCopyFileToContainer(
                      MountableFile.forHostPath(certFile),
                      "/var/run/secrets/kubernetes.io/serviceaccount/ca.crt"));

      testResourceProvider(
          attributes ->
              attributes
                  .containsEntry(
                      ResourceAttributes.CLOUD_PLATFORM.getKey(),
                      attributeValue(ResourceAttributes.CloudPlatformValues.AWS_EKS))
                  .containsEntry(
                      ResourceAttributes.K8S_CLUSTER_NAME.getKey(), attributeValue("2")));

    } finally {
      Files.delete(tokenFile);
      Files.delete(certFile);
    }
  }

  @Test
  void lambda() {

    startTestApp(
        container ->
            container
                .withEnv("AWS_REGION", "somewhere")
                .withEnv("AWS_LAMBDA_FUNCTION_NAME", "my_function")
                .withEnv("AWS_LAMBDA_FUNCTION_VERSION", "42"));

    testResourceProvider(
        attributes ->
            attributes
                .containsEntry(
                    ResourceAttributes.CLOUD_PLATFORM.getKey(),
                    attributeValue(ResourceAttributes.CloudPlatformValues.AWS_LAMBDA))
                .containsEntry(ResourceAttributes.FAAS_NAME.getKey(), attributeValue("my_function"))
                .containsEntry(ResourceAttributes.FAAS_VERSION.getKey(), attributeValue("42")));
  }

  @Test
  void ecs() {

    mockEcsMetadata();

    startTestApp(
        container ->
            container.withEnv(
                "ECS_CONTAINER_METADATA_URI_V4",
                String.format("http://%s:%d/ecs/v4", MOCK_SERVER_HOST, MOCK_SERVER_PORT)));

    testResourceProvider(
        attributes ->
            attributes.containsEntry(
                ResourceAttributes.CLOUD_PLATFORM.getKey(),
                attributeValue(ResourceAttributes.CloudPlatformValues.AWS_ECS)));
  }

  private void testResourceProvider(ResourceAttributesCheck check) {
    doRequest(getUrl("/health"), okResponseBody("Alive!"));

    checkTracesResources(check, waitForTraces());
  }

  private static void mockEc2Metadata() {
    // TODO when testing async resource loading, we need to configure extra delay on response, which
    // should not slow down the app startup if the async loading works as expected

    mockServerClient
        .when(HttpRequest.request().withMethod("PUT").withPath("/latest/api/token"))
        .respond(HttpResponse.response().withBody("token-1234"));

    mockServerClient
        .when(
            HttpRequest.request()
                .withMethod("GET")
                .withPath("/latest/dynamic/instance-identity/document"))
        .respond(
            HttpResponse.response()
                .withBody(
                    """
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

    mockServerClient
        .when(HttpRequest.request().withMethod("GET").withPath("/latest/meta-data/hostname"))
        .respond(HttpResponse.response().withBody("ec2-hostname"));
  }

  private void mockEcsMetadata() {

    // https://docs.aws.amazon.com/AmazonECS/latest/developerguide/task-metadata-endpoint-v4.html
    // we only implement the v4 format here and assume others are already covered

    mockServerClient
        .when(HttpRequest.request().withMethod("GET").withPath("/ecs/v4"))
        .respond(
            HttpResponse.response()
                .withBody(
                    """
            {
                "DockerId": "ea32192c8553fbff06c9340478a2ff089b2bb5646fb718b4ee206641c9086d66",
                "Name": "curl",
                "DockerName": "ecs-curltest-24-curl-cca48e8dcadd97805600",
                "Image": "111122223333.dkr.ecr.us-west-2.amazonaws.com/curltest:latest",
                "ImageID": "sha256:d691691e9652791a60114e67b365688d20d19940dde7c4736ea30e660d8d3553",
                "Labels": {
                    "com.amazonaws.ecs.cluster": "default",
                    "com.amazonaws.ecs.container-name": "curl",
                    "com.amazonaws.ecs.task-arn": "arn:aws:ecs:us-west-2:111122223333:task/default/8f03e41243824aea923aca126495f665",
                    "com.amazonaws.ecs.task-definition-family": "curltest",
                    "com.amazonaws.ecs.task-definition-version": "24"
                },
                "DesiredStatus": "RUNNING",
                "KnownStatus": "RUNNING",
                "Limits": {
                    "CPU": 10,
                    "Memory": 128
                },
                "CreatedAt": "2020-10-02T00:15:07.620912337Z",
                "StartedAt": "2020-10-02T00:15:08.062559351Z",
                "Type": "NORMAL",
                "LogDriver": "awslogs",
                "LogOptions": {
                    "awslogs-create-group": "true",
                    "awslogs-group": "/ecs/metadata",
                    "awslogs-region": "us-west-2",
                    "awslogs-stream": "ecs/curl/8f03e41243824aea923aca126495f665"
                },
                "ContainerARN": "arn:aws:ecs:us-west-2:111122223333:container/0206b271-b33f-47ab-86c6-a0ba208a70a9",
                "Networks": [
                    {
                        "NetworkMode": "awsvpc",
                        "IPv4Addresses": [
                            "10.0.2.100"
                        ],
                        "AttachmentIndex": 0,
                        "MACAddress": "0e:9e:32:c7:48:85",
                        "IPv4SubnetCIDRBlock": "10.0.2.0/24",
                        "PrivateDNSName": "ip-10-0-2-100.us-west-2.compute.internal",
                        "SubnetGatewayIpv4Address": "10.0.2.1/24"
                    }
                ]
            }
        """));

    mockServerClient
        .when(HttpRequest.request().withMethod("GET").withPath("/ecs/v4/task"))
        .respond(
            HttpResponse.response()
                .withBody(
                    """
        {
            "Cluster": "default",
            "TaskARN": "arn:aws:ecs:us-west-2:111122223333:task/default/158d1c8083dd49d6b527399fd6414f5c",
            "Family": "curltest",
            "ServiceName": "MyService",
            "Revision": "26",
            "DesiredStatus": "RUNNING",
            "KnownStatus": "RUNNING",
            "PullStartedAt": "2020-10-02T00:43:06.202617438Z",
            "PullStoppedAt": "2020-10-02T00:43:06.31288465Z",
            "AvailabilityZone": "us-west-2d",
            "VPCID": "vpc-1234567890abcdef0",
            "LaunchType": "EC2",
            "Containers": [
                {
                    "DockerId": "598cba581fe3f939459eaba1e071d5c93bb2c49b7d1ba7db6bb19deeb70d8e38",
                    "Name": "~internal~ecs~pause",
                    "DockerName": "ecs-curltest-26-internalecspause-e292d586b6f9dade4a00",
                    "Image": "amazon/amazon-ecs-pause:0.1.0",
                    "ImageID": "",
                    "Labels": {
                        "com.amazonaws.ecs.cluster": "default",
                        "com.amazonaws.ecs.container-name": "~internal~ecs~pause",
                        "com.amazonaws.ecs.task-arn": "arn:aws:ecs:us-west-2:111122223333:task/default/158d1c8083dd49d6b527399fd6414f5c",
                        "com.amazonaws.ecs.task-definition-family": "curltest",
                        "com.amazonaws.ecs.task-definition-version": "26"
                    },
                    "DesiredStatus": "RESOURCES_PROVISIONED",
                    "KnownStatus": "RESOURCES_PROVISIONED",
                    "Limits": {
                        "CPU": 0,
                        "Memory": 0
                    },
                    "CreatedAt": "2020-10-02T00:43:05.602352471Z",
                    "StartedAt": "2020-10-02T00:43:06.076707576Z",
                    "Type": "CNI_PAUSE",
                    "Networks": [
                        {
                            "NetworkMode": "awsvpc",
                            "IPv4Addresses": [
                                "10.0.2.61"
                            ],
                            "AttachmentIndex": 0,
                            "MACAddress": "0e:10:e2:01:bd:91",
                            "IPv4SubnetCIDRBlock": "10.0.2.0/24",
                            "PrivateDNSName": "ip-10-0-2-61.us-west-2.compute.internal",
                            "SubnetGatewayIpv4Address": "10.0.2.1/24"
                        }
                    ]
                },
                {
                    "DockerId": "ee08638adaaf009d78c248913f629e38299471d45fe7dc944d1039077e3424ca",
                    "Name": "curl",
                    "DockerName": "ecs-curltest-26-curl-a0e7dba5aca6d8cb2e00",
                    "Image": "111122223333.dkr.ecr.us-west-2.amazonaws.com/curltest:latest",
                    "ImageID": "sha256:d691691e9652791a60114e67b365688d20d19940dde7c4736ea30e660d8d3553",
                    "Labels": {
                        "com.amazonaws.ecs.cluster": "default",
                        "com.amazonaws.ecs.container-name": "curl",
                        "com.amazonaws.ecs.task-arn": "arn:aws:ecs:us-west-2:111122223333:task/default/158d1c8083dd49d6b527399fd6414f5c",
                        "com.amazonaws.ecs.task-definition-family": "curltest",
                        "com.amazonaws.ecs.task-definition-version": "26"
                    },
                    "DesiredStatus": "RUNNING",
                    "KnownStatus": "RUNNING",
                    "Limits": {
                        "CPU": 10,
                        "Memory": 128
                    },
                    "CreatedAt": "2020-10-02T00:43:06.326590752Z",
                    "StartedAt": "2020-10-02T00:43:06.767535449Z",
                    "Type": "NORMAL",
                    "LogDriver": "awslogs",
                    "LogOptions": {
                        "awslogs-create-group": "true",
                        "awslogs-group": "/ecs/metadata",
                        "awslogs-region": "us-west-2",
                        "awslogs-stream": "ecs/curl/158d1c8083dd49d6b527399fd6414f5c"
                    },
                    "ContainerARN": "arn:aws:ecs:us-west-2:111122223333:container/abb51bdd-11b4-467f-8f6c-adcfe1fe059d",
                    "Networks": [
                        {
                            "NetworkMode": "awsvpc",
                            "IPv4Addresses": [
                                "10.0.2.61"
                            ],
                            "AttachmentIndex": 0,
                            "MACAddress": "0e:10:e2:01:bd:91",
                            "IPv4SubnetCIDRBlock": "10.0.2.0/24",
                            "PrivateDNSName": "ip-10-0-2-61.us-west-2.compute.internal",
                            "SubnetGatewayIpv4Address": "10.0.2.1/24"
                        }
                    ]
                }
            ]
        }
        """));
  }
}
