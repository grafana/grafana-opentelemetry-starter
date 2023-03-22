package com.grafana.opentelemetry;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "grafana.otlp")
public class GrafanaProperties {

    private String endpoint;
    private String protocol = "http/protobuf";
    private int instanceID;
    private String apiKey;
    private final Map<String, String> resourceAttributes = new HashMap<>();

    private boolean consoleLogging;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public int getInstanceID() {
        return instanceID;
    }

    public void setInstanceID(int instanceID) {
        this.instanceID = instanceID;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public boolean isConsoleLogging() {
        return consoleLogging;
    }

    public void setConsoleLogging(boolean consoleLogging) {
        this.consoleLogging = consoleLogging;
    }

    public Map<String, String> getResourceAttributes() {
        return resourceAttributes;
    }
}
