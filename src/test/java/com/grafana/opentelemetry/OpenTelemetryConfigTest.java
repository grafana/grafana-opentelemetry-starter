package com.grafana.opentelemetry;

import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

class OpenTelemetryConfigTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("overrideCases")
    void updateResourceAttribute(String name, String expected, String explicit, String[] override) {
        HashMap<String, String> resourceAttributes = new HashMap<>();
        if (explicit != null) {
            resourceAttributes.put(ResourceAttributes.SERVICE_NAME.getKey(), explicit);
        }
        OpenTelemetryConfig.updateResourceAttribute(resourceAttributes, ResourceAttributes.SERVICE_NAME,
                override);

        if (expected == null) {
            Assertions.assertThat(resourceAttributes).isEmpty();
        } else {
            Assertions.assertThat(resourceAttributes)
                    .containsExactlyEntriesOf(Map.of(ResourceAttributes.SERVICE_NAME.getKey(), expected));
        }
    }

    private static Stream<Arguments> overrideCases() {
        return Stream.of(
                Arguments.of("explicit name is kept", "explicit", "explicit", new String[] { "ignored" }),
                Arguments.of("only override is used", "override", null, new String[] { "override" }),
                Arguments.of("first non-blank override is used", "override", null, new String[] { " ", "override" }),
                Arguments.of("first non-empty override is used", "override", null, new String[] { "", "override" }),
                Arguments.of("first non-null override is used", "override", null, new String[] { null, "override" }),
                Arguments.of("no value found", null, null, new String[] { " ", null })
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("basicAuthCases")
    void getBasicAuthHeader(String name, String expected, String apiKey, int instanceId) {
        String basicAuthHeader = OpenTelemetryConfig.getBasicAuthHeader(instanceId, apiKey);
        Assertions.assertThat(basicAuthHeader).isEqualTo(expected);
    }

    private static Stream<Arguments> basicAuthCases() {
        return Stream.of(
                Arguments.of("valid basic auth", "Authorization=Basic MTIyMzQ1OmFwaUtleQ==", "apiKey", 122345),
                Arguments.of("API key blank", "", " ", 12345),
                Arguments.of("instanceId 0", "", "apiKey", 0)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpointCases")
    void getEndpoint(String name, String expected, String zone, String endpoint) {
        Assertions.assertThat(OpenTelemetryConfig.getEndpoint(endpoint, zone)).isEqualTo(expected);
    }

    private static Stream<Arguments> endpointCases() {
        return Stream.of(
                Arguments.of("only zone", "https://otlp-gateway-zone.grafana.net/otlp", "zone", ""),
                Arguments.of("only endpoint", "endpoint", "", "endpoint"),
                Arguments.of("both", "endpoint", "zone", "endpoint")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("maskCases")
    void maskAuthHeader(String name, Map<String, String> expected, Map<String, String> given) {
        Map<String, String> map = OpenTelemetryConfig.maskAuthHeader(given);
        Assertions.assertThat(map).containsExactlyInAnyOrderEntriesOf(expected);
    }

    private static Stream<Arguments> maskCases() {
        return Stream.of(
                Arguments.of("masked",
                        Map.of(
                                "foo", "bar",
                                "otel.exporter.otlp.headers", "Authorization=Basic NTUz..."),
                        Map.of(
                                "foo", "bar",
                                "otel.exporter.otlp.headers", "Authorization=Basic NTUzMzg2OmV5SnJJam9pW")),
                Arguments.of("short auth header",
                        Map.of(
                                "foo", "bar",
                                "otel.exporter.otlp.headers", ""),
                        Map.of(
                                "foo", "bar",
                                "otel.exporter.otlp.headers", "")),
                Arguments.of("no auth header",
                        Map.of(
                                "foo", "bar"),
                        Map.of(
                                "foo", "bar"))
        );
    }

}
