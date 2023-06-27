package com.grafana.autoconfigure.internal;

import com.grafana.autoconfigure.PropertiesAdapter;

import javax.annotation.Nullable;
import java.util.Map;

public class GrafanaProps {

    private final PropertiesAdapter adapter;
    private final CloudProperties cloud;
    private final OnPremProperties onPrem;

    public GrafanaProps(PropertiesAdapter adapter) {
        this.adapter = adapter;
        this.cloud = new CloudProperties(adapter);
        this.onPrem = new OnPremProperties(adapter);
    }

    /**
     * Log all metrics, traces, and logs that are created for debugging purposes
     * (in addition to sending them to the backend via OTLP).
     * <p>
     * This will also send metrics and traces to Loki as an unintended side effect.
     */
    public boolean isDebugLogging() {
        return adapter.getBoolean("grafana.otlp.debug.logging", false);
    }

    @Nullable
    public Map<String, String> getGlobalAttributes() {
        return adapter.getMap("grafana.otlp.globalAttributes");
    }

    public CloudProperties getCloud() {
        return cloud;
    }

    public OnPremProperties getOnPrem() {
        return onPrem;
    }

    public static class CloudProperties {

        private final PropertiesAdapter adapter;

        public CloudProperties(PropertiesAdapter adapter) {
            this.adapter = adapter;
        }

        /**
         * Zone used when creating your Grafana cloud stack
         * <p>
         * Use <code>onprem.endpoint</code> instead of <code>zone</code> when using the Grafana Agent.
         */
        @Nullable
        public String getZone() {
            return adapter.getString("grafana.otlp.cloud.zone");
        }

        public int getInstanceId() {
            return adapter.getInt("grafana.otlp.cloud.instance_id", 0);
        }

        /**
         * The API Key to securely publish signal data to grafana.com cloud stack.
         * <p>
         * Leave <code>ApiKey</code> empty when using the Grafana Agent.
         */
        @Nullable
        public String getApiKey() {
            return adapter.getString("grafana.otlp.cloud.api-key");
        }

        /**
         * Protocol is http/protobuf for grafana cloud stack
         */
        public String getProtocol() {
            return "http/protobuf";
        }
    }

    public static class OnPremProperties {

        private final PropertiesAdapter adapter;

        public OnPremProperties(PropertiesAdapter adapter) {
            this.adapter = adapter;
        }

        /**
         * The endpoint of the Grafana Agent.
         */
        @Nullable
        public String getEndpoint() {
            return adapter.getString("grafana.otlp.onprem.endpoint");
        }

        @Nullable
        public String getProtocol() {
            return adapter.getString("grafana.otlp.onprem.protocol");
        }

    }
}