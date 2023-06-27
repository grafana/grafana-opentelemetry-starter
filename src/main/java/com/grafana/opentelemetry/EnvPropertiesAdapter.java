package com.grafana.opentelemetry;

import com.grafana.opentelemetry.properties.PropertiesAdapter;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

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

    @SuppressWarnings({"rawtypes", "DataFlowIssue"})
    @Override
    public Map<String, String> getStringMap(String key) {
        return ((AbstractEnvironment) environment).getPropertySources().stream()
                .filter(ps -> ps instanceof EnumerablePropertySource)
                .map(ps -> ((EnumerablePropertySource) ps).getPropertyNames())
                .flatMap(Arrays::stream)
                .filter(propName -> propName.contains(key))
                .collect(Collectors.toMap(
                        p -> p.substring(p.lastIndexOf(key) + (key.length() + 1)),
                        environment::getProperty));
    }
}
