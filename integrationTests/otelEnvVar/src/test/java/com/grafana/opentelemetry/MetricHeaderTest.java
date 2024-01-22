package com.grafana.opentelemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {HelloController.class, DemoApplication.class})
@AutoConfigureObservability
@SetEnvironmentVariable(key = "OTEL_EXPORTER_OTLP_HEADERS", value = "Authorization=Basic%20NTUz")
class MetricHeaderTest {

  @SuppressWarnings("unused")
  @Autowired
  private MetricsOtlpConfig metricsOtlpConfig;

  @Test
  void usesOtelHeaderFromEnvVar() {
    assertThat(metricsOtlpConfig.headers()).containsExactly(entry("Authorization", "Basic NTUz"));
  }
}
