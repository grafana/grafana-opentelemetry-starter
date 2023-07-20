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
   *   <li>environment variable OTEL_SERVICE_NAME
   *   <li>environment variable OTEL_RESOURCE_ATTRIBUTES
   *   <li>Manually set service_name in grafana.otlp.globalAttributes
   *   <li>spring.application.name" in application.properties
   *   <li>'Implementation-Title' in jar's MANIFEST.MF
   * </ol>
   *
   * <p>The following block can be added to build.gradle to set the application name and version in
   * the jar's MANIFEST.MF:
   *
   * <pre>
   * bootJar {
   *     manifest {
   *         attributes('Implementation-Title':   'Demo Application',
   *                    'Implementation-Version':  version)
   *     }
   * }
   * </pre>
   *
   * The <code>service.instance.id</code> attribute will be set if any of the following return a
   * value. The list is in order of precedence.
   *
   * <ol>
   *   <li>InetAddress.getLocalHost().getHostName()
   *   <li>environment variable HOSTNAME
   *   <li>environment variable HOST
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

  /**
   * Enable or disable the OpenTelemetry integration (default is enabled).
   *
   * <p>This can be used to disable the integration without removing the dependency.
   */
  private boolean enabled = true;

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

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
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
     * locally with the default gRPC endpoint (localhost:4317).
     *
     * <p>Use <code>cloud.zone</code> instead of <code>endpoint</code> when using the Grafana Cloud.
     */
    private String endpoint;

    /**
     * The protocol used to send OTLP data. Can be either <code>http/protobuf</code> or <code>grpc
     * </code> (default).
     */
    private String protocol;

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
  }
}
