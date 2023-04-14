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
                Arguments.of("explicit name is kept", "explicit", "explicit", new String[]{"ignored"}),
                Arguments.of("only override is used", "override", null, new String[]{"override"}),
                Arguments.of("first non-blank override is used", "override", null, new String[]{" ", "override"}),
                Arguments.of("first non-null override is used", "override", null, new String[]{null, "override"}),
                Arguments.of("no value found", null, null, new String[]{" ", null})
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
}
