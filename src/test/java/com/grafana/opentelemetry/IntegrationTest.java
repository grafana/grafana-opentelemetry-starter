package com.grafana.opentelemetry;

import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.HttpRequest;
import org.mockserver.springtest.MockServerTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

@SpringBootTest(
        classes = { HelloController.class, DemoApplication.class },
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@MockServerTest
@AutoConfigureObservability
@TestPropertySource(properties = {
        "grafana.otlp.onprem.endpoint = http://localhost:${mockServerPort}",
        "grafana.otlp.onprem.protocol = grpc" //is overridden by system property otel.exporter.otlp.protocol
})
class IntegrationTest {

    private MockServerClient mockServerClient;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private Environment environment;

    @Autowired
    private Optional<AutoConfiguredOpenTelemetrySdk> sdk;

    static {
        String delay = "500";
        System.setProperty("otel.metric.export.interval", delay);
        System.setProperty("otel.bsp.schedule.delay", delay);
        System.setProperty("otel.exporter.otlp.protocol", "http/protobuf");
    }

    @Test
    void testProperties() {
        Assertions.assertThat(OpenTelemetryConfig.getGrafanaProperties(environment).getCloud().getZone()).isEqualTo("prod-eu-west-0");
    }

    @Test
    void systemPropHasPriority() {
        Assertions.assertThat(sdk).hasValueSatisfying(
                v -> Assertions.assertThat(v.getConfig().getString("otel.exporter.otlp.protocol"))
                             .isEqualTo("http/protobuf"));
    }

    @Test
    void dataIsSent() throws InterruptedException {
        restTemplate.getForEntity("/hello", String.class);

        await().atMost(10, SECONDS).untilAsserted(() -> {
                    verifyPath("/v1/traces");
                    verifyPath("/v1/metrics");
                    verifyPath("/v1/logs");
                }
        );
    }

    private void verifyPath(String path) {
        // only assert that a request was received,
        // because the goal of this test is to make sure that data is still sent when dependabot upgrades
        // spring boot, which can also update the OpenTelemetry version
        mockServerClient.verify(HttpRequest.request()
                                        .withMethod(HttpMethod.POST.name())
                                        .withPath(path)
                                        .withHeader("Content-Type", "application/x-protobuf")
        );
    }
}
