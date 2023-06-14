package com.grafana.opentelemetry.properties;

public interface PropertiesAdapter {
    Boolean getBoolean(String key);

    Integer getInteger(String key);

    String getString(String key);
}
