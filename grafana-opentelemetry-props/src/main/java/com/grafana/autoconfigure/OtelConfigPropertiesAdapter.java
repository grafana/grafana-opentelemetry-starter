package com.grafana.autoconfigure;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;

import java.util.HashMap;
import java.util.Map;

public class OtelConfigPropertiesAdapter implements PropertiesAdapter {

    private final ConfigProperties props;

    public OtelConfigPropertiesAdapter(ConfigProperties props) {
        this.props = props;
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        return props.getBoolean(key, defaultValue);
    }

    @Override
    public Boolean getBoolean(String key) {
        return props.getBoolean(key);
    }

    @Override
    public int getInt(String key, int defaultValue) {
        return props.getInt(key, defaultValue);
    }

    @Override
    public Integer getInt(String key) {
        return props.getInt(key);
    }


    @Override
    public String getString(String key) {
        return props.getString(key);
    }

    @Override
    public String getString(String key, String defaultValue) {
        return props.getString(key, defaultValue);
    }

    @Override
    public Map<String, String> getMap(String key) {
        return props.getMap(key, new HashMap<>());
    }
}
