package com.grafana.opentelemetry;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "grafana.otlp")
public class GrafanaProperties {

    /**
     * The grafana cloud OTLP gateway endpoint in the form of
     * <code>https://otlp-gateway-<Grafana region>.grafana.net/otlp</code>
     */
    private String endpoint;

    /**
     * The protocol used to send OTLP data. Can be either <code>http/protobuf</code> (which is the default)
     * or <code>grpc</code>.
     */
    private String protocol = "http/protobuf";

    /**
     * The Instance ID can be found when you click on "Details" in the "Grafana" section on grafana.com.
     * <p>
     * Leave this field empty when using the Grafana OSS stack.
     */
    private int instanceId;

    /**
     * Create an API key under "Security" / "API Keys" (left side navigation tree) on grafana.com.
     * The role should be "MetricsPublisher"
     * <p>
     * Leave this field empty when using the Grafana OSS stack.
     */
    private String apiKey;

    /**
     * Adds global (resource) attributes to metrics, traces and logs.
     * <p>
     * For example, you can add <code>service.version</code> to make it easier to see if a new version of the
     * application is causing a problem.
     * <p>
     * If you add "spring.application.name" to application.properties, the application name will be copied to
     * <code>service.name</code>, but you can override <code>service.name</code>.
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
     */
    private final Map<String, String> globalAttributes = new HashMap<>();

    /**
     * Log all metrics, traces, and logs that are created for debugging purposes
     * (in addition to sending them to the backend via OTLP).
     * <p>
     * This will also send metrics and traces to Loki as an unintended side effect.
     */
    private boolean debug;

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

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public Map<String, String> getGlobalAttributes() {
        return globalAttributes;
    }
}
