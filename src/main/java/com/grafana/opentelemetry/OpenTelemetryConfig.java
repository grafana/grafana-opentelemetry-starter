package com.grafana.opentelemetry;

import static org.springframework.util.ReflectionUtils.findMethod;
import static org.springframework.util.ReflectionUtils.invokeMethod;

import io.micrometer.common.util.StringUtils;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.logging.LoggingMeterRegistry;
import io.micrometer.registry.otlp.OtlpMeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.ResourceAttributes;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(value = "grafana.otlp.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(GrafanaProperties.class)
@PropertySource(value = {"classpath:grafana-otel-starter.properties"})
public class OpenTelemetryConfig {

  public static final String PROTOCOL = "http/protobuf";

  private static final Logger logger = LoggerFactory.getLogger(OpenTelemetryConfig.class);

  public static final String OTLP_HEADERS = "otel.exporter.otlp.headers";

  private static final Set<String> EXCLUDED_ATTRIBUTES =
      Set.of(
          ResourceAttributes.TELEMETRY_SDK_NAME.getKey(),
          ResourceAttributes.TELEMETRY_SDK_LANGUAGE.getKey(),
          ResourceAttributes.TELEMETRY_SDK_VERSION.getKey());

  @Bean
  public Resource getResource(AutoConfiguredOpenTelemetrySdk sdk) {
    Method getResource = findMethod(AutoConfiguredOpenTelemetrySdk.class, "getResource");
    Objects.requireNonNull(getResource).setAccessible(true);
    return (Resource) invokeMethod(getResource, sdk);
  }

  @Bean
  public MetricsOtlpConfig metricsOtlpConfig(
      Resource resource, ConnectionProperties connectionProperties) {
    return new MetricsOtlpConfig(
        getMap(resource, k -> !EXCLUDED_ATTRIBUTES.contains(k)), connectionProperties);
  }

  @Bean
  public OtlpMeterRegistry openTelemetryMeterRegistry(
      Clock clock, MetricsOtlpConfig metricsOtlpConfig) {
    return new OtlpMeterRegistry(metricsOtlpConfig, clock);
  }

  static Map<String, String> getMap(Resource resource, Predicate<String> filter) {
    return resource.getAttributes().asMap().entrySet().stream()
        .filter(e -> filter.test(e.getKey().getKey()))
        .collect(Collectors.toMap(t -> t.getKey().getKey(), e -> e.getValue().toString()));
  }

  @Bean
  @ConditionalOnProperty(
      value = "grafana.otlp.debugLogging",
      havingValue = "true",
      matchIfMissing = false)
  public LoggingMeterRegistry loggingMeterRegistry() {
    return new LoggingMeterRegistry();
  }

  @Bean
  public OpenTelemetry openTelemetry(
      Optional<AutoConfiguredOpenTelemetrySdk> sdk,
      List<LogAppenderConfigurer> logAppenderConfigurers) {
    OpenTelemetry openTelemetry =
        sdk.<OpenTelemetry>map(AutoConfiguredOpenTelemetrySdk::getOpenTelemetrySdk)
            .orElse(OpenTelemetry.noop());
    tryAddAppender(openTelemetry, logAppenderConfigurers);
    return openTelemetry;
  }

  static void tryAddAppender(
      OpenTelemetry openTelemetry, List<LogAppenderConfigurer> logAppenderConfigurers) {
    if (logAppenderConfigurers.isEmpty()) {
      logger.warn("no logging library found - OpenTelemetryAppender not added");
    } else {
      logAppenderConfigurers.forEach(
          logAppenderConfigurer -> logAppenderConfigurer.tryAddAppender(openTelemetry));
    }
  }

  @Bean
  public AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk(
      GrafanaProperties properties,
      ConnectionProperties connectionProperties,
      @Value("${spring.application.name:#{null}}") String applicationName) {
    // the log record exporter uses the global instance, so we need to set it as global to avoid a
    // warning
    AutoConfiguredOpenTelemetrySdkBuilder builder =
        AutoConfiguredOpenTelemetrySdk.builder().setResultAsGlobal();

    Map<String, String> configProperties = getConfigProperties(properties, connectionProperties);
    builder.addPropertiesSupplier(() -> configProperties);
    builder.addResourceCustomizer(
        (resource, unused) -> {
          // the provided resource takes precedence over spring resource,
          // because it contains the service.name that is specified in otel.service.name
          return springResource(properties, applicationName, resource);
        });

    logger.info("using config properties: {}", maskAuthHeader(configProperties));

    try {
      return builder.build();
    } catch (Exception e) {
      logger.warn("unable to create OpenTelemetry instance", e);
      return null;
    }
  }

  private Map<String, String> getConfigProperties(
      GrafanaProperties properties, ConnectionProperties connectionProperties) {
    String exporters = properties.isDebugLogging() ? "logging,otlp" : "otlp";

    Map<String, String> configProperties =
        new HashMap<>(
            Map.of(
                "otel.exporter.otlp.protocol", PROTOCOL,
                "otel.traces.exporter", exporters,
                "otel.logs.exporter", exporters));
    if (!connectionProperties.headers().isEmpty()) {
      configProperties.put(
          OTLP_HEADERS,
          connectionProperties.headers().entrySet().stream()
              .map(e -> String.format("%s=%s", e.getKey(), e.getValue()))
              .collect(Collectors.joining(",")));
    }
    connectionProperties
        .endpoint()
        .ifPresent(s -> configProperties.put("otel.exporter.otlp.endpoint", s));
    return configProperties;
  }

  @Bean
  public ConnectionProperties connectionProperties(
      GrafanaProperties properties,
      @Value("${otel.exporter.otlp.endpoint:#{null}}") String otlpEndpoint,
      @Value("${otel.metric.export.interval:60000}") String metricExportInterval) {
    GrafanaProperties.CloudProperties cloud = properties.getCloud();
    Map<String, String> headers = getHeaders(cloud.getInstanceId(), cloud.getApiKey());
    if (StringUtils.isBlank(otlpEndpoint)) {
      otlpEndpoint = properties.getOnPrem().getEndpoint();
    }
    Optional<String> endpoint = getEndpoint(otlpEndpoint, cloud.getZone(), headers);

    return new ConnectionProperties(
        endpoint, headers, Duration.of(Integer.parseInt(metricExportInterval), ChronoUnit.MILLIS));
  }

  static Map<String, String> maskAuthHeader(Map<String, String> configProperties) {
    return configProperties.entrySet().stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                e -> {
                  String v = e.getValue();
                  return e.getKey().equals(OTLP_HEADERS) && v.length() > 24
                      ? v.substring(0, 24) + "..."
                      : v;
                }));
  }

  static Optional<String> getEndpoint(String endpoint, String zone, Map<String, String> headers) {
    boolean hasZone = Strings.isNotBlank(zone);
    boolean hasEndpoint = Strings.isNotBlank(endpoint);
    if (headers.isEmpty()) {
      if (hasZone) {
        logger.warn(
            "ignoring grafana.otlp.cloud.zone, because grafana.otlp.cloud.instanceId was not found");
      }
      if (hasEndpoint) {
        return Optional.of(endpoint);
      } else {
        logger.info("grafana.otlp.onprem.endpoint not found, using default endpoint");
      }
    } else {
      if (hasEndpoint) {
        logger.warn(
            "ignoring grafana.otlp.onprem.endpoint, because grafana.otlp.cloud.instanceId was found");
      }
      if (hasZone) {
        return Optional.of(String.format("https://otlp-gateway-%s.grafana.net/otlp", zone));
      } else {
        logger.warn("please specify grafana.otlp.cloud.zone");
      }
    }

    return Optional.empty();
  }

  static Map<String, String> getHeaders(int instanceId, String apiKey) {
    boolean hasKey = Strings.isNotBlank(apiKey);
    boolean hasId = instanceId != 0;
    if (hasKey && hasId) {
      String userPass = String.format("%s:%s", instanceId, apiKey);
      return Map.of(
          "Authorization",
          String.format("Basic %s", Base64.getEncoder().encodeToString(userPass.getBytes())));
    }

    if (hasKey) {
      logger.warn("found grafana.otlp.cloud.apiKey but no grafana.otlp.cloud.instanceId");
    }
    if (hasId) {
      logger.warn("found grafana.otlp.cloud.instanceId but no grafana.otlp.cloud.apiKey");
    }

    return otlpMetricHeaders();
  }

  /**
   * copied from
   * https://github.com/micrometer-metrics/micrometer/blob/8a2196cd30d301bfab9c2a69e212fe926fb2035d/implementations/micrometer-registry-otlp/src/main/java/io/micrometer/registry/otlp/OtlpConfig.java#L156-L182
   *
   * <p>Can be called directly once https://github.com/micrometer-metrics/micrometer/pull/4500 is
   * merged
   */
  private static Map<String, String> otlpMetricHeaders() {
    Map<String, String> env = System.getenv();
    // common headers
    String headersString = env.getOrDefault("OTEL_EXPORTER_OTLP_HEADERS", "").trim();
    String metricsHeaders = env.getOrDefault("OTEL_EXPORTER_OTLP_METRICS_HEADERS", "").trim();
    headersString =
        Objects.equals(headersString, "") ? metricsHeaders : headersString + "," + metricsHeaders;
    try {
      // headers are encoded as URL - see
      // https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/protocol/exporter.md#specifying-headers-via-environment-variables
      headersString = URLDecoder.decode(headersString, StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new IllegalArgumentException("Cannot decode header value: " + headersString, e);
    }

    String[] keyValues =
        Objects.equals(headersString, "") ? new String[] {} : headersString.split(",");

    return Arrays.stream(keyValues)
        .map(String::trim)
        .filter(keyValue -> keyValue.length() > 2 && keyValue.indexOf('=') > 0)
        .collect(
            Collectors.toMap(
                keyValue -> keyValue.substring(0, keyValue.indexOf('=')).trim(),
                keyValue -> keyValue.substring(keyValue.indexOf('=') + 1).trim(),
                (l, r) -> r));
  }

  private static Resource springResource(
      GrafanaProperties properties, String applicationName, Resource provided) {
    String globalName =
        properties.getGlobalAttributes().get(ResourceAttributes.SERVICE_NAME.getKey());
    if (StringUtils.isNotBlank(globalName)) {
      applicationName = globalName;
    }

    AttributesBuilder b = io.opentelemetry.api.common.Attributes.builder();
    properties.getGlobalAttributes().forEach((k, v) -> b.put(AttributeKey.stringKey(k), v));
    Resource spring = Resource.create(b.build());
    Resource merged = spring.merge(provided);
    if ("unknown_service:java".equals(provided.getAttribute(ResourceAttributes.SERVICE_NAME))
        && StringUtils.isNotBlank(applicationName)) {
      return merged.merge(
          Resource.create(
              io.opentelemetry.api.common.Attributes.of(
                  ResourceAttributes.SERVICE_NAME, applicationName)));
    }
    return merged;
  }
}
