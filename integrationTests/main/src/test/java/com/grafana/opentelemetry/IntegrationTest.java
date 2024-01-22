package com.grafana.opentelemetry;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.registry.otlp.OtlpMeterRegistry;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.springtest.MockServerTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(
    classes = {HelloController.class, DemoApplication.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@MockServerTest
@AutoConfigureObservability
@TestPropertySource(
    properties = {
      "grafana.otlp.onprem.endpoint = http://localhost:${mockServerPort}",
    })
class IntegrationTest {

  @SuppressWarnings("unused")
  private MockServerClient mockServerClient;

  @Autowired private TestRestTemplate restTemplate;

  @Autowired private GrafanaProperties properties;

  @SuppressWarnings("unused")
  @Autowired
  private ConnectionProperties connectionProperties;

  @Autowired private OtlpMeterRegistry meterRegistry;

  @Autowired private Optional<AutoConfiguredOpenTelemetrySdk> sdk;

  @Test
  void testProperties() {
    assertThat(properties.getCloud().getZone()).isEqualTo("prod-eu-west-0");
  }

  @Test
  void metricsResourceAttributes() throws IllegalAccessException {
    List<Map.Entry<String, String>> metricsResourceAttributes = getMetricsResourceAttributes();
    assertThat(metricsResourceAttributes)
        .contains(Map.entry("telemetry.sdk.name", "io.micrometer"));
    assertThat(metricsResourceAttributes).extracting(Map.Entry::getKey).doesNotHaveDuplicates();
  }

  @Test
  void usesHttp() {
    assertThat(sdk)
        .hasValueSatisfying(
            v -> {
              try {
                ConfigProperties p =
                    (ConfigProperties) MethodUtils.invokeMethod(v, true, "getConfig");
                assertThat(p.getString("otel.exporter.otlp.protocol")).isEqualTo("http/protobuf");
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            });
  }

  private List<Map.Entry<String, String>> getMetricsResourceAttributes()
      throws IllegalAccessException {

    io.opentelemetry.proto.resource.v1.Resource resource =
        (io.opentelemetry.proto.resource.v1.Resource)
            FieldUtils.readField(meterRegistry, "resource", true);

    return resource.getAttributesList().stream()
        .map(a -> Map.entry(a.getKey(), a.getValue().getStringValue()))
        .toList();
  }
}
