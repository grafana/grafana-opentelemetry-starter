package com.grafana.opentelemetry;

import com.grafana.opentelemetry.properties.PropertiesAdapter;
import org.springframework.core.env.Environment;

class EnvPropertiesAdapter implements PropertiesAdapter {
    private final Environment environment;

    public EnvPropertiesAdapter(Environment environment) {
        this.environment = environment;
    }

    @Override
    public Boolean getBoolean(String key) {
        return environment.getProperty(key, Boolean.TYPE);
    }

    @Override
    public Integer getInteger(String key) {
        return environment.getProperty(key, Integer.class);
    }

    @Override
    public String getString(String key) {
        return environment.getProperty(key, String.class);
    }
}
