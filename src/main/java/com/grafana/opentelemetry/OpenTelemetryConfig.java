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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

@Configuration
public class OpenTelemetryConfig {

    private static final Logger logger = LoggerFactory.getLogger(OpenTelemetryConfig.class);

    public static final String OTLP_HEADERS = "otel.exporter.otlp.headers";

    @Bean
    public MeterRegistry openTelemetryMeterRegistry(OpenTelemetry openTelemetry, Clock clock) {
        return OpenTelemetryMeterRegistry.builder(openTelemetry).setClock(clock).build();
    }

    @Bean
    public OpenTelemetry openTelemetry(GrafanaProperties properties,
            @Value("${spring.application.name:#{null}}") String applicationName) {
        AutoConfiguredOpenTelemetrySdkBuilder builder = AutoConfiguredOpenTelemetrySdk.builder();

        Map<String, String> configProperties = getConfigProperties(properties, applicationName);
        builder.addPropertiesSupplier(() -> configProperties);
        logger.info("using config properties: {}", maskAuthHeader(configProperties));

        try {
            return builder.build().getOpenTelemetrySdk();
        } catch (Exception e) {
            logger.warn("unable to create OpenTelemetry instance", e);
            return OpenTelemetry.noop();
        }
    }

    private static Map<String, String> getConfigProperties(GrafanaProperties properties, String applicationName) {
        String exporters = properties.isDebugLogging() ? "logging,otlp" : "otlp";

        GrafanaProperties.CloudProperties cloud = properties.getCloud();
        Optional<String> authHeader = getBasicAuthHeader(cloud.getInstanceId(), cloud.getApiKey());
        Map<String, String> configProperties = new HashMap<>(Map.of(
                "otel.resource.attributes", getResourceAttributes(properties, applicationName),
                "otel.exporter.otlp.protocol", getProtocol(properties.getProtocol(), authHeader),
                "otel.traces.exporter", exporters,
                "otel.metrics.exporter", exporters,
                "otel.logs.exporter", exporters
        ));
        authHeader.ifPresent(s -> configProperties.put(OTLP_HEADERS, s));
        getEndpoint(properties.getOnPrem().getEndpoint(), cloud.getZone(), authHeader.isPresent())
                .ifPresent(s -> configProperties.put("otel.exporter.otlp.endpoint", s));
        return configProperties;
    }

    private static String getProtocol(String protocol, Optional<String> authHeader) {
        if (Strings.isNotBlank(protocol)) {
            return protocol;
        }
        return authHeader.isPresent() ? "http/protobuf" : "grpc";
    }

    static Map<String, String> maskAuthHeader(Map<String, String> configProperties) {
        return configProperties.entrySet()
                       .stream()
                       .collect(Collectors.toMap(
                               Map.Entry::getKey,
                               e -> {
                                   String v = e.getValue();
                                   return e.getKey().equals(OTLP_HEADERS) && v.length() > 24 ?
                                                  v.substring(0, 24) + "..." : v;
                               }));
    }

    static Optional<String> getEndpoint(String endpoint, String zone, boolean cloud) {
        if (Strings.isNotBlank(endpoint)) {
            return Optional.of(endpoint);
        }
        if (Strings.isNotBlank(zone)) {
            return Optional.of(String.format("https://otlp-gateway-%s.grafana.net/otlp", zone));
        }
        if (cloud) {
            logger.warn("please specify either grafana.otlp.onprem.endpoint or grafana.otlp.cloud.zone");
        }
        return Optional.empty();
    }

    static Optional<String> getBasicAuthHeader(int instanceId, String apiKey) {
        if (Strings.isBlank(apiKey) || instanceId == 0) {
            return Optional.empty();
        }
        String userPass = String.format("%s:%s", instanceId, apiKey);
        return Optional.of(
                String.format("Authorization=Basic %s", Base64.getEncoder().encodeToString(userPass.getBytes())));
    }

    private static String getResourceAttributes(GrafanaProperties properties, String applicationName) {
        Map<String, String> resourceAttributes = properties.getGlobalAttributes();

        String manifestApplicationName = null;
        String manifestApplicationVersion = null;
        try {
            Manifest mf = new Manifest();
            mf.read(ClassLoader.getSystemResourceAsStream("META-INF/MANIFEST.MF"));
            Attributes atts = mf.getMainAttributes();

            Object n = atts.getValue("Implementation-Title");
            if (n != null) {
                manifestApplicationName = n.toString();
            }
            Object v = atts.getValue("Implementation-Version");
            if (v != null) {
                manifestApplicationVersion = v.toString();
            }
        } catch (Exception e) {
            // ignore error reading manifest
        }

        updateResourceAttribute(resourceAttributes, ResourceAttributes.SERVICE_NAME, applicationName,
                manifestApplicationName);
        updateResourceAttribute(resourceAttributes, ResourceAttributes.SERVICE_VERSION, manifestApplicationVersion);
        updateResourceAttribute(resourceAttributes, ResourceAttributes.SERVICE_INSTANCE_ID, System.getenv("HOSTNAME"),
                System.getenv("HOST"));

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
