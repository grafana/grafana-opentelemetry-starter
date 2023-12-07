package com.grafana.opentelemetry;

import io.opentelemetry.api.OpenTelemetry;

public interface LogAppenderConfigurer {
  void tryAddAppender(OpenTelemetry openTelemetry);
}
