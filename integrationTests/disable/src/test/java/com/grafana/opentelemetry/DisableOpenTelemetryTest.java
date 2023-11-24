package com.grafana.opentelemetry;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SuppressWarnings("SpringBootApplicationProperties")
@SpringBootTest(classes = {HelloController.class, DemoApplication.class, OpenTelemetryConfig.class})
@AutoConfigureObservability
@TestPropertySource(
    properties = {
      "grafana.otlp.enabled = false",
    })
public class DisableOpenTelemetryTest {

  @Test
  void starterIsNotApplied() {
    // we could also check that no data is sent, but this would require us to wait a certain amount
    // of time
    // e.g. 10 seconds - and this would make the test slow and complicated
    Assertions.assertThat(LogbackConfig.hasAppender(LogbackConfig.getLogger())).isFalse();
  }
}
