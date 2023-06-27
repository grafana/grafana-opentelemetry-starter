package com.grafana.opentelemetry.properties;

import java.util.Map;

public interface PropertiesAdapter {
    Boolean getBoolean(String key);

    Integer getInteger(String key);

    String getString(String key);

    Map<String, String> getStringMap(String key);
}
