package com.grafana.opentelemetry;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "grafana.otlp")
public class GrafanaProperties {

  private CloudProperties cloud = new CloudProperties();

  private OnPremProperties onPrem = new OnPremProperties();

  /**
   * Adds global (resource) attributes to metrics, traces and logs.
   *
   * <p>For example, you can add <code>service.version</code> to make it easier to see if a new
   * version of the application is causing a problem.
   *
   * <p>The attributes <code>service.name</code>, <code>service.version</code>, and <code>
   * service.instance.id</code> are automatically detected as outlined below.
   *
   * <p>For <code>service.name</code> the order of precedence is:
   *
   * <ol>
   *   <li>Manually set otel.service.name (not possible in spring properties)
   *   <li>Manually set service.name in otel.resource.attributes (not possible in spring properties)
   *   <li>Manually set service.name in grafana.otlp.globalAttributes
   *   <li>spring.application.name" in application.properties
   * </ol>
   */
  private final Map<String, String> globalAttributes = new HashMap<>();

  /**
   * Log all metrics, traces, and logs that are created for debugging purposes (in addition to
   * sending them to the backend via OTLP).
   *
   * <p>This will also send metrics and traces to Loki as an unintended side effect.
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
     *
     * <p>Use <code>onprem.endpoint</code> instead of <code>zone</code> when using the Grafana
     * Agent.
     */
    private String zone;

    /**
     * The Instance ID can be found when you click on "Details" in the "Grafana" section on
     * grafana.com.
     *
     * <p>Leave <code>instanceId</code> empty when using the Grafana Agent.
     */
    private int instanceId;

    /**
     * Create an API key under "Security" / "API Keys" (left side navigation tree) on grafana.com.
     * The role should be "MetricsPublisher"
     *
     * <p>Leave <code>apiKey</code> empty when using the Grafana Agent.
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
     * The endpoint of the Grafana Agent.
     *
     * <p>You do not need to set an <code>endpoint</code> value if your Grafana Agent is running
     * locally with the default http/protobuf endpoint (http://localhost:4318).
     *
     * <p>Use <code>cloud.zone</code> instead of <code>endpoint</code> when using the Grafana Cloud.
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
