package com.grafana.opentelemetry;

import java.util.Map;
import java.util.Optional;

class TraslatedProperties {
    private Map<String, String> headers;
    private Optional<String> endpoint;
    private Map<String, String> resourceAttributes;

    public TraslatedProperties(Optional<String> endpoint, Map<String, String> headers, Map<String, String> resourceAttributes) {
        this.headers = headers;
        this.endpoint = endpoint;
        this.resourceAttributes = resourceAttributes;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Optional<String> getEndpoint() {
        return endpoint;
    }

    public Map<String, String> getResourceAttributes() {
        return resourceAttributes;
    }
}
