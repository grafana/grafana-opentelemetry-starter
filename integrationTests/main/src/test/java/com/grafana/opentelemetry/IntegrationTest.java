package com.grafana.opentelemetry;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.util.Optional;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.HttpRequest;
import org.mockserver.springtest.MockServerTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(
    classes = {HelloController.class, DemoApplication.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@MockServerTest
@AutoConfigureObservability
@TestPropertySource(
    properties = {
      "grafana.otlp.onprem.endpoint = http://localhost:${mockServerPort}",
      "grafana.otlp.onprem.protocol = grpc" // is overridden by system property
      // otel.exporter.otlp.protocol
    })
class IntegrationTest {

  @SuppressWarnings("unused")
  private MockServerClient mockServerClient;

  @Autowired private TestRestTemplate restTemplate;

  @Autowired private GrafanaProperties properties;

  @Autowired private Optional<AutoConfiguredOpenTelemetrySdk> sdk;

  static {
    String delay = "500";
    System.setProperty("otel.metric.export.interval", delay);
    System.setProperty("otel.bsp.schedule.delay", delay);
    System.setProperty("otel.exporter.otlp.protocol", "http/protobuf");
  }

  @Test
  void testProperties() {
    Assertions.assertThat(properties.getCloud().getZone()).isEqualTo("prod-eu-west-0");
  }

  @Test
  void systemPropHasPriority() {
    Assertions.assertThat(sdk)
        .hasValueSatisfying(
            v -> {
              try {
                ConfigProperties p =
                    (ConfigProperties) MethodUtils.invokeMethod(v, true, "getConfig");
                Assertions.assertThat(p.getString("otel.exporter.otlp.protocol"))
                    .isEqualTo("http/protobuf");
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            });
  }

  @Test
  void dataIsSent() {
    restTemplate.getForEntity("/hello", String.class);

    await()
        .atMost(10, SECONDS)
        .untilAsserted(
            () -> {
              verifyPath("/v1/traces");
              verifyPath("/v1/metrics");
              verifyPath("/v1/logs");
            });
  }

  private void verifyPath(String path) {
    // only assert that a request was received,
    // because the goal of this test is to make sure that data is still sent when dependabot
    // upgrades
    // spring boot, which can also update the OpenTelemetry version
    mockServerClient.verify(
        HttpRequest.request()
            .withMethod(HttpMethod.POST.name())
            .withPath(path)
            .withHeader("Content-Type", "application/x-protobuf"));
  }
}
