package com.grafana.opentelemetry;

import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@ExtendWith(OutputCaptureExtension.class)
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
    void getBasicAuthHeader(String name, Optional<String> expected, String expectedOutput,
            String apiKey, int instanceId, CapturedOutput output) {
        Optional<String> basicAuthHeader = OpenTelemetryConfig.getBasicAuthHeader(instanceId, apiKey);
        Assertions.assertThat(basicAuthHeader).isEqualTo(expected);
        Assertions.assertThat(output).contains(expectedOutput);
    }

    private static Stream<Arguments> basicAuthCases() {
        return Stream.of(
                Arguments.of("valid basic auth",
                        Optional.of("Authorization=Basic MTIyMzQ1OmFwaUtleQ=="), "",
                        "apiKey", 122345),
                Arguments.of("API key and instanceId missing",
                        Optional.empty(), "",
                        " ", 12345),
                Arguments.of("API key blank",
                        Optional.empty(), "found grafana.otlp.cloud.instanceId but no grafana.otlp.cloud.apiKey",
                        " ", 12345),
                Arguments.of("instanceId 0",
                        Optional.empty(), "found grafana.otlp.cloud.apiKey but no grafana.otlp.cloud.instanceId",
                        "apiKey", 0)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpointCases")
    void getEndpoint(String name,
                     Optional<String> expected,
                     String expectedOutput,
                     String zone,
                     String endpoint,
                     Optional<String> authHeader,
                     CapturedOutput output) {
        Assertions.assertThat(OpenTelemetryConfig.getEndpoint(endpoint, zone, authHeader)).isEqualTo(expected);
        Assertions.assertThat(output).contains(expectedOutput);
    }

    private static Stream<Arguments> endpointCases() {
        return Stream.of(
                Arguments.of("only zone",
                        Optional.of("https://otlp-gateway-zone.grafana.net/otlp"), "",
                        "zone", "", Optional.of("apiKey")),
                Arguments.of("only onprem endpoint",
                        Optional.of("endpoint"), "",
                        "", "endpoint", Optional.empty()),
                Arguments.of("both with cloud",
                        Optional.of("https://otlp-gateway-zone.grafana.net/otlp"),
                        "ignoring grafana.otlp.onprem.endpoint, because grafana.otlp.cloud.instanceId was found",
                        "zone", "endpoint", Optional.of("key")),
                Arguments.of("zone without instanceId",
                        Optional.of("endpoint"),
                        "ignoring grafana.otlp.cloud.zone, because grafana.otlp.cloud.instanceId was not found",
                        "zone", "endpoint", Optional.empty()),
                Arguments.of("missing zone",
                        Optional.empty(),
                        "please specify grafana.otlp.cloud.zone",
                        " ", " ", Optional.of("key")),
                Arguments.of("onprem endpoint not set",
                        Optional.empty(),
                        "grafana.otlp.onprem.endpoint not found, using default endpoint for otel.exporter.otlp.protocol",
                        " ", " ", Optional.empty())
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("protocolCases")
    void getProtocol(String name, String expected, String expectedOutput,
            String protocol, Optional<String> authHeader, CapturedOutput output) {
        Assertions.assertThat(OpenTelemetryConfig.getProtocol(protocol, authHeader)).isEqualTo(expected);
        Assertions.assertThat(output).contains(expectedOutput);
    }

    private static Stream<Arguments> protocolCases() {
        return Stream.of(
                Arguments.of("cloud",
                        "http/protobuf", "",
                        "", Optional.of("apiKey")),
                Arguments.of("cloud and proto",
                        "http/protobuf",
                        "ignoring grafana.otlp.onprem.protocol, because grafana.otlp.cloud.instanceId was found",
                        "grpc", Optional.of("apiKey")),
                Arguments.of("onprem",
                        "grpc", "",
                        "", Optional.empty()),
                Arguments.of("onprem and proto",
                        "http/protobuf", "",
                        "http/protobuf", Optional.empty())
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
                                OpenTelemetryConfig.OTLP_HEADERS, "Authorization=Basic NTUz..."),
                        Map.of(
                                "foo", "bar",
                                OpenTelemetryConfig.OTLP_HEADERS, "Authorization=Basic NTUzMzg2OmV5SnJJam9pW")),
                Arguments.of("short auth header",
                        Map.of(
                                "foo", "bar",
                                OpenTelemetryConfig.OTLP_HEADERS, ""),
                        Map.of(
                                "foo", "bar",
                                OpenTelemetryConfig.OTLP_HEADERS, "")),
                Arguments.of("no auth header",
                        Map.of(
                                "foo", "bar"),
                        Map.of(
                                "foo", "bar"))
        );
    }

}
