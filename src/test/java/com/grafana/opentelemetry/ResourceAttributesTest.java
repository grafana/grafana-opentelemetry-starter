package com.grafana.opentelemetry;

import static org.junit.jupiter.api.Named.named;

import io.micrometer.core.instrument.Clock;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.sdk.resources.Resource;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.util.ReflectionUtils;

public class ResourceAttributesTest {

  static class TestCase {
    Map<String, String> expected;
    private Function<ApplicationContextRunner, ApplicationContextRunner> runner;

    public TestCase(Map<String, String> expected) {
      this.expected = expected;
    }

    public TestCase withRunner(
        Function<ApplicationContextRunner, ApplicationContextRunner> runner) {
      this.runner = runner;
      return this;
    }
  }

  @AfterEach
  void setUp() throws Exception {
    GlobalOpenTelemetry.resetForTest();
    ReflectionUtils.invokeMethod(
        Class.forName("io.opentelemetry.api.events.GlobalEventEmitterProvider")
            .getMethod("resetForTest"),
        null);
  }

  @ParameterizedTest
  @MethodSource("testCases")
  void readResourceAttributes(TestCase testCase) {
    ApplicationContextRunner runner =
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OpenTelemetryConfig.class))
            .withBean(Clock.class, () -> Clock.SYSTEM);
    if (testCase.runner != null) {
      runner = testCase.runner.apply(runner);
    }
    runner.run(
        context -> {
          Resource config = context.getBean(Resource.class);
          Assertions.assertThat(OpenTelemetryConfig.getMap(config, k -> true))
              .containsAllEntriesOf(testCase.expected);
        });
  }

  private static Stream<Arguments> testCases() {
    return Stream.of(
        Arguments.of(
            named(
                "nothing provided",
                new TestCase(
                    Map.of(
                        "service.name", "unknown_service:java",
                        "telemetry.sdk.name", "opentelemetry",
                        "telemetry.distro.name", "grafana-opentelemetry-starter")))),
        Arguments.of(
            named(
                "application name in spring properties",
                new TestCase(Map.of("service.name", "spring-name"))
                    .withRunner(
                        runner ->
                            runner.withPropertyValues("spring.application.name=spring-name")))),
        Arguments.of(
            named(
                "application name in global attributes",
                new TestCase(Map.of("service.name", "global-name"))
                    .withRunner(
                        runner ->
                            runner.withPropertyValues(
                                "spring.application.name=spring-name",
                                "grafana.otlp.global-attributes.service.name=global-name")))),
        Arguments.of(
            named(
                "application name in otel.resource.attributes",
                new TestCase(Map.of("service.name", "res-name"))
                    .withRunner(
                        runner ->
                            runner
                                .withSystemProperties(
                                    "otel.resource.attributes=service.name=res-name")
                                .withPropertyValues(
                                    "spring.application.name=spring-name",
                                    "grafana.otlp.global-attributes.service.name=global-name")))),
        Arguments.of(
            named(
                "application name in otel.resource.attributes",
                new TestCase(Map.of("service.name", "service-name"))
                    .withRunner(
                        runner ->
                            runner
                                .withSystemProperties(
                                    "otel.resource.attributes=service.name=res-name",
                                    "otel.service.name=service-name")
                                .withPropertyValues(
                                    "spring.application.name=spring-name",
                                    "grafana.otlp.global-attributes.service.name=global-name")))));
  }
}
