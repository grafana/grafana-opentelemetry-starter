package com.grafana.opentelemetry;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.instrumentation.micrometer.v1_5.OpenTelemetryMeterRegistry;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class OpenTelemetryConfig {

    @Bean
    public MeterRegistry openTelemetryMeterRegistry(OpenTelemetry openTelemetry, Clock clock) {
        return OpenTelemetryMeterRegistry.builder(openTelemetry).setClock(clock).build();
    }

    @Bean
    public OpenTelemetry openTelemetry(GrafanaProperties properties,
            @Value("${spring.application.name}") String applicationName) {
        String userPass = String.format("%s:%s", properties.getInstanceId(), properties.getApiKey());
        String auth = String.format("Authorization=Basic %s", Base64.getEncoder().encodeToString(userPass.getBytes()));

        String exporter = properties.isDebug() ? "logging,otlp" : "otlp";

        AutoConfiguredOpenTelemetrySdkBuilder builder = AutoConfiguredOpenTelemetrySdk.builder();

        builder.addPropertiesSupplier(() -> Map.of(
                "otel.resource.attributes", getResourceAttributes(properties, applicationName),
                "otel.exporter.otlp.protocol", properties.getProtocol(),
                "otel.exporter.otlp.endpoint", properties.getEndpoint(),
                "otel.exporter.otlp.headers", auth,
                "otel.traces.exporter", exporter,
                "otel.metrics.exporter", exporter,
                "otel.logs.exporter", exporter
        ));

        return builder.build().getOpenTelemetrySdk();
    }

    private static String getResourceAttributes(GrafanaProperties properties, String applicationName) {
        Map<String, String> resourceAttributes = properties.getGlobalAttributes();

        Package p = OpenTelemetryConfig.class.getPackage();
        updateResourceAttribute(resourceAttributes, ResourceAttributes.SERVICE_NAME, applicationName, p.getImplementationTitle());
        updateResourceAttribute(resourceAttributes, ResourceAttributes.SERVICE_VERSION, p.getImplementationVersion());

        return resourceAttributes.entrySet().stream()
                       .map(e -> String.format("%s=%s", e.getKey(), e.getValue()))
                       .collect(Collectors.joining(","));
    }

    static void updateResourceAttribute(Map<String, String> resourceAttributes,
            AttributeKey<String> key, String... overrides) {

        if (!resourceAttributes.containsKey(key.getKey())) {
            for (String value : overrides) {
                if (Strings.isNotBlank(value)) {
                    resourceAttributes.put(key.getKey(), value);
                    return;
                }
            }
        }
    }
}
