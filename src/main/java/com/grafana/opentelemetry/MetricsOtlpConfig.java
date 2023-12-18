package com.grafana.opentelemetry;

import io.micrometer.registry.otlp.OtlpConfig;
import java.util.Map;
import java.util.concurrent.TimeUnit;

class MetricsOtlpConfig implements OtlpConfig {

  private final Map<String, String> resourceAttributes;
  private final ConnectionProperties connectionProperties;

  public MetricsOtlpConfig(
      Map<String, String> resourceAttributes, ConnectionProperties connectionProperties) {
    this.resourceAttributes = resourceAttributes;
    this.connectionProperties = connectionProperties;
  }

  @Override
  public Map<String, String> resourceAttributes() {
    return resourceAttributes;
  }

  @Override
  public String url() {
    return connectionProperties
        .getEndpoint()
        .map(s -> s + "/v1/metrics")
        .orElse(OtlpConfig.DEFAULT.url());
  }

  @Override
  public Map<String, String> headers() {
    return connectionProperties.getHeaders();
  }

  @Override
  public String get(String key) {
    return null;
  }

  @Override
  public TimeUnit baseTimeUnit() {
    return TimeUnit.SECONDS;
  }
}
