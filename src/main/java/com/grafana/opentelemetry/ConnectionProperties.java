package com.grafana.opentelemetry;

import java.util.Map;
import java.util.Optional;

class ConnectionProperties {
  private final Map<String, String> headers;
  private final Optional<String> endpoint;

  public ConnectionProperties(Optional<String> endpoint, Map<String, String> headers) {
    this.headers = headers;
    this.endpoint = endpoint;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public Optional<String> getEndpoint() {
    return endpoint;
  }
}
