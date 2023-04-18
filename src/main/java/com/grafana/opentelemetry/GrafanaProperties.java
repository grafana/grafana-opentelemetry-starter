package com.grafana.opentelemetry;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "grafana.otlp")
public class GrafanaProperties {

    private CloudProperties cloud = new CloudProperties();

    private OnPremProperties onPrem = new OnPremProperties();


    /**
     * The protocol used to send OTLP data. Can be either <code>http/protobuf</code> (which is the default)
     * or <code>grpc</code>.
     */
    private String protocol = "http/protobuf";

    /**
     * Adds global (resource) attributes to metrics, traces and logs.
     * <p>
     * For example, you can add <code>service.version</code> to make it easier to see if a new version of the
     * application is causing a problem.
     * <p>
     * The attributes <code>service.name</code>, <code>service.version</code>, and <code>service.instance.id</code>
     * are automatically detected as explained below, but if you set the value manually, it will be respected.
     * <p>
     * "spring.application.name" in application.properties will be translated to <code>service.name</code>.
     * <p>
     * You can also add the application name and version to MANIFEST.MF, where they will be copied to
     * <code>service.name</code> and <code>service.version</code> respectively.
     * <p>
     * In gradle, the application name and version can be set as follows:
     * <pre>
     * bootJar {
     *     manifest {
     *         attributes('Implementation-Title':   'Demo Application',
     *                    'Implementation-Version': version)
     *     }
     * }
     * </pre>
     * The environment variables HOST or HOSTNAME will be translated to <code>service.instance.id</code>.
     */
    private final Map<String, String> globalAttributes = new HashMap<>();

    /**
     * Log all metrics, traces, and logs that are created for debugging purposes
     * (in addition to sending them to the backend via OTLP).
     * <p>
     * This will also send metrics and traces to Loki as an unintended side effect.
     */
    private boolean debugLogging;

    public CloudProperties getCloud() {
        return cloud;
    }

    public void setCloud(CloudProperties cloud) {
        this.cloud = cloud;
    }

    public OnPremProperties getOnPrem() {
        return onPrem;
    }

    public void setOnPrem(OnPremProperties onPrem) {
        this.onPrem = onPrem;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public boolean isDebugLogging() {
        return debugLogging;
    }

    public void setDebugLogging(boolean debugLogging) {
        this.debugLogging = debugLogging;
    }

    public Map<String, String> getGlobalAttributes() {
        return globalAttributes;
    }

    public static class CloudProperties {
        /**
         * The Zone can be found when you click on "Details" in the "Grafana" section on grafana.com.
         * <p>
         * Use <code>endpoint</code> instead of <code>zone</code> when using the Grafana OSS stack.
         */
        private String zone;

        /**
         * The Instance ID can be found when you click on "Details" in the "Grafana" section on grafana.com.
         * <p>
         * Leave <code>instanceId</code> empty when using the Grafana OSS stack.
         */
        private int instanceId;

        /**
         * Create an API key under "Security" / "API Keys" (left side navigation tree) on grafana.com.
         * The role should be "MetricsPublisher"
         * <p>
         * Leave <code>apiKey</code> empty when using the Grafana OSS stack.
         */
        private String apiKey;

        public String getZone() {
            return zone;
        }

        public void setZone(String zone) {
            this.zone = zone;
        }

        public int getInstanceId() {
            return instanceId;
        }

        public void setInstanceId(int instanceId) {
            this.instanceId = instanceId;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
    }

    public static class OnPremProperties {
        /**
         * When using the Grafana OSS stack, set the endpoint to the grafana agent URL.
         * <p>
         * Use <code>zone</code> instead of <code>endpoint</code> when using the Grafana Cloud.
         */
        private String endpoint;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }
    }
}
