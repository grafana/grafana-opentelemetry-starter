package com.grafana.opentelemetry;

import io.micrometer.registry.otlp.OtlpConfig;

import java.util.Map;
import java.util.concurrent.TimeUnit;

class MetricsOtlpConfig implements OtlpConfig {

    private final TranslatedProperties p;

    public MetricsOtlpConfig(TranslatedProperties p) {
        this.p = p;
    }

    @Override
    public Map<String, String> resourceAttributes() {
        return p.getResourceAttributes();
    }

    @Override
    public String url() {
        return p.getEndpoint().map(s -> s + "/v1/metrics").orElse(OtlpConfig.DEFAULT.url());
    }

    @Override
    public Map<String, String> headers() {
        return p.getHeaders();
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
