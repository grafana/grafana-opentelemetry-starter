package com.grafana.opentelemetry;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.instrumentation.micrometer.v1_5.OpenTelemetryMeterRegistry;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.InstrumentSelector;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.View;
import io.opentelemetry.sdk.metrics.internal.aggregator.ExplicitBucketHistogramUtils;
import io.opentelemetry.semconv.ResourceAttributes;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
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

  public static final String DISTRIBUTION_NAME = "telemetry.distro.name";
  public static final String DISTRIBUTION_VERSION = "telemetry.distro.version";

  private static final Logger logger = LoggerFactory.getLogger(OpenTelemetryConfig.class);

  public static final String OTLP_HEADERS = "otel.exporter.otlp.headers";

  @Bean
  public MeterRegistry openTelemetryMeterRegistry(OpenTelemetry openTelemetry, Clock clock) {
    // note: add setting histogramGaugesEnabled in new otel version
    return OpenTelemetryMeterRegistry.builder(openTelemetry)
        .setClock(clock)
        .setBaseTimeUnit(TimeUnit.SECONDS)
        .build();
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
      @Value("${spring.application.name:#{null}}") String applicationName) {
    AutoConfiguredOpenTelemetrySdkBuilder builder = AutoConfiguredOpenTelemetrySdk.builder();
    builder.addMeterProviderCustomizer((b, configProperties) -> customizeMeterBuilder(b));

    Map<String, String> configProperties = getConfigProperties(properties, applicationName);
    builder.addPropertiesSupplier(() -> configProperties);
    logger.info("using config properties: {}", maskAuthHeader(configProperties));

    try {
      return builder.build();
    } catch (Exception e) {
      logger.warn("unable to create OpenTelemetry instance", e);
      return null;
    }
  }

  private static SdkMeterProviderBuilder customizeMeterBuilder(
      SdkMeterProviderBuilder meterProviderBuilder) {
    // workaround for bug that bucket boundaries are not scaled correctly: bucket boundaries for
    // seconds
    List<Double> buckets =
        ExplicitBucketHistogramUtils.DEFAULT_HISTOGRAM_BUCKET_BOUNDARIES.stream()
            .map(d -> d * 0.001)
            .collect(Collectors.toList());

    meterProviderBuilder.registerView(
        InstrumentSelector.builder().setType(InstrumentType.HISTOGRAM).build(),
        View.builder().setAggregation(Aggregation.explicitBucketHistogram(buckets)).build());
    return meterProviderBuilder;
  }

  private static Map<String, String> getConfigProperties(
      GrafanaProperties properties, String applicationName) {
    String exporters = properties.isDebugLogging() ? "logging,otlp" : "otlp";

    GrafanaProperties.CloudProperties cloud = properties.getCloud();
    GrafanaProperties.OnPremProperties onPrem = properties.getOnPrem();
    Optional<String> authHeader = getBasicAuthHeader(cloud.getInstanceId(), cloud.getApiKey());
    Map<String, String> configProperties =
        new HashMap<>(
            Map.of(
                "otel.resource.attributes", getResourceAttributes(properties, applicationName),
                "otel.exporter.otlp.protocol", getProtocol(onPrem.getProtocol(), authHeader),
                "otel.traces.exporter", exporters,
                "otel.metrics.exporter", exporters,
                "otel.logs.exporter", exporters));
    authHeader.ifPresent(s -> configProperties.put(OTLP_HEADERS, s));
    getEndpoint(onPrem.getEndpoint(), cloud.getZone(), authHeader)
        .ifPresent(s -> configProperties.put("otel.exporter.otlp.endpoint", s));
    return configProperties;
  }

  static String getProtocol(String protocol, Optional<String> authHeader) {
    boolean hasProto = Strings.isNotBlank(protocol);
    if (authHeader.isPresent()) {
      if (hasProto) {
        logger.warn(
            "ignoring grafana.otlp.onprem.protocol, because grafana.otlp.cloud.instanceId was found");
      }
      return "http/protobuf";
    }

    return hasProto ? protocol : "grpc";
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

  static Optional<String> getEndpoint(String endpoint, String zone, Optional<String> authHeader) {
    boolean hasZone = Strings.isNotBlank(zone);
    boolean hasEndpoint = Strings.isNotBlank(endpoint);
    if (authHeader.isPresent()) {
      if (hasEndpoint) {
        logger.warn(
            "ignoring grafana.otlp.onprem.endpoint, because grafana.otlp.cloud.instanceId was found");
      }
      if (hasZone) {
        return Optional.of(String.format("https://otlp-gateway-%s.grafana.net/otlp", zone));
      } else {
        logger.warn("please specify grafana.otlp.cloud.zone");
      }
    } else {
      if (hasZone) {
        logger.warn(
            "ignoring grafana.otlp.cloud.zone, because grafana.otlp.cloud.instanceId was not found");
      }
      if (hasEndpoint) {
        return Optional.of(endpoint);
      } else {
        logger.info(
            "grafana.otlp.onprem.endpoint not found, using default endpoint for otel.exporter.otlp.protocol");
      }
    }
    return Optional.empty();
  }

  static Optional<String> getBasicAuthHeader(int instanceId, String apiKey) {
    boolean hasKey = Strings.isNotBlank(apiKey);
    boolean hasId = instanceId != 0;
    if (hasKey && hasId) {
      String userPass = String.format("%s:%s", instanceId, apiKey);
      return Optional.of(
          String.format(
              "Authorization=Basic %s", Base64.getEncoder().encodeToString(userPass.getBytes())));
    }

    if (hasKey) {
      logger.warn("found grafana.otlp.cloud.apiKey but no grafana.otlp.cloud.instanceId");
    }
    if (hasId) {
      logger.warn("found grafana.otlp.cloud.instanceId but no grafana.otlp.cloud.apiKey");
    }

    return Optional.empty();
  }

  private static String getResourceAttributes(
      GrafanaProperties properties, String applicationName) {
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

    updateResourceAttribute(
        resourceAttributes,
        ResourceAttributes.SERVICE_NAME,
        applicationName,
        manifestApplicationName);
    updateResourceAttribute(
        resourceAttributes, ResourceAttributes.SERVICE_VERSION, manifestApplicationVersion);

    String hostName;
    try {
      hostName = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      hostName = System.getenv("HOSTNAME");
    }
    updateResourceAttribute(
        resourceAttributes,
        ResourceAttributes.SERVICE_INSTANCE_ID,
        hostName,
        System.getenv("HOST"));

    resourceAttributes.put(DISTRIBUTION_NAME, "grafana-opentelemetry-starter");
    resourceAttributes.put(DISTRIBUTION_VERSION, DistributionVersion.VERSION);

    return resourceAttributes.entrySet().stream()
        .map(e -> String.format("%s=%s", e.getKey(), e.getValue()))
        .collect(Collectors.joining(","));
  }

  static void updateResourceAttribute(
      Map<String, String> resourceAttributes, AttributeKey<String> key, String... overrides) {

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
