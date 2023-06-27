package com.grafana.autoconfigure;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class SpringEnvironmentPropertiesAdapter implements PropertiesAdapter {
    private final Environment env;

    public SpringEnvironmentPropertiesAdapter(Environment env) {
        this.env = env;
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        return env.getProperty(key, Boolean.class, defaultValue);
    }

    public Boolean getBoolean(String key) {
        return env.getProperty(key, Boolean.class);
    }

    @Override
    public int getInt(String key, int defaultValue) {
        return env.getProperty(key, Integer.class, defaultValue);
    }

    public Integer getInt(String key) {
        return env.getProperty(key, Integer.class);
    }

    public String getString(String key) {
        return env.getProperty(key, String.class);
    }

    @Override
    public String getString(String key, String defaultValue) {
        return env.getProperty(key, String.class, defaultValue);
    }

    @Override
    public Map<String, String> getMap(String key) {
        MutablePropertySources propSrcs = ((AbstractEnvironment) env).getPropertySources();
        return StreamSupport.stream(propSrcs.spliterator(), false)
                .filter(ps -> ps instanceof EnumerablePropertySource)
                .map(ps -> ((EnumerablePropertySource) ps).getPropertyNames())
                .flatMap(Arrays::stream)
                .filter(propName -> propName.contains(key))
                .collect(Collectors.toMap(
                        p -> p.substring(p.lastIndexOf(key) + (key.length()+1)),
                        p -> StringUtils.defaultIfBlank(env.getProperty(p),"")));
    }
}
