package com.grafana.opentelemetry;

import com.grafana.opentelemetry.properties.GrafanaProperties;
import com.grafana.opentelemetry.properties.PropertiesReader;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.micrometer.v1_5.OpenTelemetryMeterRegistry;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Configuration(proxyBeanMethods = false)
public class OpenTelemetryConfig {

    private static final Logger logger = LoggerFactory.getLogger(OpenTelemetryConfig.class);

    @Bean
    public MeterRegistry openTelemetryMeterRegistry(OpenTelemetry openTelemetry, Clock clock) {
        return OpenTelemetryMeterRegistry.builder(openTelemetry)
                .setClock(clock)
                .setBaseTimeUnit(TimeUnit.SECONDS)
                .build();
    }

    @Bean
    public OpenTelemetry openTelemetry(Optional<AutoConfiguredOpenTelemetrySdk> sdk) {
        OpenTelemetry openTelemetry = sdk.<OpenTelemetry>map(AutoConfiguredOpenTelemetrySdk::getOpenTelemetrySdk)
                .orElse(OpenTelemetry.noop());
        addLogAppender(openTelemetry);
        return openTelemetry;
    }

    private void addLogAppender(OpenTelemetry openTelemetry) {
        //the openTelemetry object is not used yet, but it will be used in the future, when the global otel instance is not used by default anymore

        if (!LogbackConfig.tryAddAppender() && !Log4jConfig.tryAddAppender()) {
            logger.warn("no logging library found - OpenTelemetryAppender not added");
        }
    }

    @Bean
    public AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk(
            Environment environment, @Value("${spring.application.name:#{null}}") String applicationName) {

        GrafanaProperties properties = getGrafanaProperties(environment);
        AutoConfiguredOpenTelemetrySdkBuilder builder = AutoConfiguredOpenTelemetrySdk.builder();

        Map<String, String> configProperties = PropertiesReader.getConfigProperties(properties, applicationName);
        builder.addPropertiesSupplier(() -> configProperties);
        logger.info("using config properties: {}", PropertiesReader.maskAuthHeader(configProperties));

        try {
            return builder.build();
        } catch (Exception e) {
            logger.warn("unable to create OpenTelemetry instance", e);
            return null;
        }
    }

    static GrafanaProperties getGrafanaProperties(Environment environment) {
        return PropertiesReader.getGrafanaProperties(new EnvPropertiesAdapter(environment));
    }
}
