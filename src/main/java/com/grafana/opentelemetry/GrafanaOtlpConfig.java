package com.grafana.opentelemetry;

import io.micrometer.registry.otlp.OtlpConfig;

import java.util.Map;

class GrafanaOtlpConfig implements OtlpConfig {

    private final TraslatedProperties p;

    public GrafanaOtlpConfig(TraslatedProperties p) {
        this.p = p;
    }

    @Override
    public Map<String, String> resourceAttributes() {
        return p.getResourceAttributes();
    }

    @Override
    public String url() {
        return p.getEndpoint().orElse(OtlpConfig.DEFAULT.url());
    }

    @Override
    public Map<String, String> headers() {
        return p.getHeaders();
    }

    @Override
    public String get(String key) {
        return null;
    }

    //todo wait for next micrometer version to expose this: https://github.com/micrometer-metrics/micrometer/pull/3883/files#diff-472b2d48e56d0063bd23f43e531f7f14f3f2305f807d2bbd66aada9f644e8f79R152-R154
//            @Override
//            public TimeUnit baseTimeUnit() {
//                return null;
//            }

}
