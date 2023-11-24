package com.grafana.opentelemetry.log4j;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

import com.grafana.opentelemetry.OpenTelemetryConfig;
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
    classes = {HelloController.class, DemoApplication.class, OpenTelemetryConfig.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@MockServerTest
@AutoConfigureObservability
@TestPropertySource(
    properties = {
      "grafana.otlp.onprem.endpoint = http://localhost:${mockServerPort}",
      "grafana.otlp.onprem.protocol = http/protobuf",
    })
public class Log4jIntegrationTest {

  @SuppressWarnings("unused")
  private MockServerClient mockServerClient;

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void logDataIsSent() {
    restTemplate.getForEntity("/hello", String.class);

    await().atMost(10, SECONDS).untilAsserted(this::verifyLogs);
  }

  private void verifyLogs() {
    // only assert that a request was received,
    // because the goal of this test is to make sure that data is still sent when dependabot
    // upgrades
    // spring boot, which can also update the OpenTelemetry version
    mockServerClient.verify(
        HttpRequest.request()
            .withMethod(HttpMethod.POST.name())
            .withPath("/v1/logs")
            .withHeader("Content-Type", "application/x-protobuf"));
  }
}
