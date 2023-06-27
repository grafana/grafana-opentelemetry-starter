package com.grafana.autoconfigure;

import java.util.Map;

public interface PropertiesAdapter {

    boolean getBoolean(String key, boolean defaultValue);

    Boolean getBoolean(String key);

    int getInt(String key, int defaultValue);

    Integer getInt(String key);

    String getString(String key);

    String getString(String key, String defaultValue);

    Map<String, String> getMap(String key);

}
