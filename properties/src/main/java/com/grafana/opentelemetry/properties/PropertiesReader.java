package com.grafana.opentelemetry.properties;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

public class PropertiesReader {

    public static final String OTLP_HEADERS = "otel.exporter.otlp.headers";
    private static final Logger logger = LoggerFactory.getLogger(PropertiesReader.class);

    public static GrafanaProperties getGrafanaProperties(PropertiesAdapter adapter) {
        return new GrafanaProperties(
                new GrafanaProperties.CloudProperties(
                        adapter.getString("grafana.otlp.cloud.zone"),
                        adapter.getInteger("grafana.otlp.cloud.instanceId"),
                        adapter.getString("grafana.otlp.cloud.apiKey")
                ),
                new GrafanaProperties.OnPremProperties(
                        adapter.getString("grafana.otlp.onprem.endpoint"),
                        adapter.getString("grafana.otlp.onprem.protocol")
                ),
                adapter.getBoolean("grafana.otlp.debugLogging"));
    }


    public static Map<String, String> getConfigProperties(GrafanaProperties properties, String applicationName) {
        String exporters = properties.isDebugLogging() ? "logging,otlp" : "otlp";

        GrafanaProperties.CloudProperties cloud = properties.getCloud();
        GrafanaProperties.OnPremProperties onPrem = properties.getOnPrem();
        Optional<String> authHeader = getBasicAuthHeader(cloud.getInstanceId(), cloud.getApiKey());
        Map<String, String> configProperties = new HashMap<>();
        configProperties.put("otel.resource.attributes", getResourceAttributes(properties, applicationName));
        configProperties.put("otel.exporter.otlp.protocol", getProtocol(onPrem.getProtocol(), authHeader));
        configProperties.put("otel.traces.exporter", exporters);
        configProperties.put("otel.metrics.exporter", exporters);
        configProperties.put("otel.logs.exporter", exporters);

        authHeader.ifPresent(s -> configProperties.put(OTLP_HEADERS, s));
        getEndpoint(onPrem.getEndpoint(), cloud.getZone(), authHeader)
                .ifPresent(s -> configProperties.put("otel.exporter.otlp.endpoint", s));
        return configProperties;
    }

    static String getProtocol(String protocol, Optional<String> authHeader) {
        boolean hasProto = Strings.isNotBlank(protocol);
        if (authHeader.isPresent()) {
            if (hasProto) {
                logger.warn("ignoring grafana.otlp.onprem.protocol, because grafana.otlp.cloud.instanceId was found");
            }
            return "http/protobuf";
        }

        return hasProto ? protocol : "grpc";
    }

    public static Map<String, String> maskAuthHeader(Map<String, String> configProperties) {
        return configProperties.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> {
                            String v = e.getValue();
                            return e.getKey().equals(OTLP_HEADERS) && v.length() > 24 ?
                                    v.substring(0, 24) + "..." : v;
                        }));
    }

    static Optional<String> getEndpoint(String endpoint, String zone, Optional<String> authHeader) {
        boolean hasZone = Strings.isNotBlank(zone);
        boolean hasEndpoint = Strings.isNotBlank(endpoint);
        if (authHeader.isPresent()) {
            if (hasEndpoint) {
                logger.warn("ignoring grafana.otlp.onprem.endpoint, because grafana.otlp.cloud.instanceId was found");
            }
            if (hasZone) {
                return Optional.of(String.format("https://otlp-gateway-%s.grafana.net/otlp", zone));
            } else {
                logger.warn("please specify grafana.otlp.cloud.zone");
            }
        } else {
            if (hasZone) {
                logger.warn("ignoring grafana.otlp.cloud.zone, because grafana.otlp.cloud.instanceId was not found");
            }
            if (hasEndpoint) {
                return Optional.of(endpoint);
            } else {
                logger.info("grafana.otlp.onprem.endpoint not found, using default endpoint for otel.exporter.otlp.protocol");
            }
        }
        return Optional.empty();
    }

    static Optional<String> getBasicAuthHeader(Integer instanceId, String apiKey) {
        boolean hasKey = Strings.isNotBlank(apiKey);
        boolean hasId = instanceId != null && instanceId > 0;
        if (hasKey && hasId) {
            String userPass = String.format("%s:%s", instanceId, apiKey);
            return Optional.of(
                    String.format("Authorization=Basic %s", Base64.getEncoder().encodeToString(userPass.getBytes())));
        }

        if (hasKey) {
            logger.warn("found grafana.otlp.cloud.apiKey but no grafana.otlp.cloud.instanceId");
        }
        if (hasId) {
            logger.warn("found grafana.otlp.cloud.instanceId but no grafana.otlp.cloud.apiKey");
        }

        return Optional.empty();
    }

    private static String getResourceAttributes(GrafanaProperties properties, String applicationName) {
        Map<String, String> resourceAttributes = properties.getGlobalAttributes();

        String manifestApplicationName = null;
        String manifestApplicationVersion = null;
        try {
            Manifest mf = new Manifest();
            mf.read(ClassLoader.getSystemResourceAsStream("META-INF/MANIFEST.MF"));
            Attributes atts = mf.getMainAttributes();

            Object n = atts.getValue("Implementation-Title");
            if (n != null) {
                manifestApplicationName = n.toString();
            }
            Object v = atts.getValue("Implementation-Version");
            if (v != null) {
                manifestApplicationVersion = v.toString();
            }
        } catch (Exception e) {
            // ignore error reading manifest
        }

        updateResourceAttribute(resourceAttributes, ResourceAttributes.SERVICE_NAME, applicationName, manifestApplicationName);
        updateResourceAttribute(resourceAttributes, ResourceAttributes.SERVICE_VERSION, manifestApplicationVersion);

        String hostName;
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostName = System.getenv("HOSTNAME");
        }
        updateResourceAttribute(resourceAttributes, ResourceAttributes.SERVICE_INSTANCE_ID,
                hostName, System.getenv("HOST"));

        return resourceAttributes.entrySet().stream()
                .map(e -> String.format("%s=%s", e.getKey(), e.getValue()))
                .collect(Collectors.joining(","));
    }

    public static void updateResourceAttribute(Map<String, String> resourceAttributes,
                                               AttributeKey<String> key, String... overrides) {

        if (!resourceAttributes.containsKey(key.getKey())) {
            for (String value : overrides) {
                if (Strings.isNotBlank(value)) {
                    resourceAttributes.put(key.getKey(), value);
                    return;
                }
            }
        }
    }
}
