package com.grafana.opentelemetry;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

public record ConnectionProperties(Optional<String> endpoint, Map<String, String> headers, Duration metricExportInterval) {}
