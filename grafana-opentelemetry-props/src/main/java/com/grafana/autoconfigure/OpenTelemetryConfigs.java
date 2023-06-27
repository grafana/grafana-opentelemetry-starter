package com.grafana.autoconfigure;

import com.google.common.base.Verify;
import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableMap;
import com.grafana.autoconfigure.internal.GrafanaProps;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenTelemetryConfigs {
    private static final Logger logger = LoggerFactory.getLogger(OpenTelemetryConfigs.class);

    public ImmutableMap<String, String> getConfigProperties(PropertiesAdapter adapter,
                                                            Map<String, List<String>> globalAttributeUpdates) {

        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        GrafanaProps props = new GrafanaProps(adapter);

        builder.putAll(getExporters(props.isDebugLogging()));
        Map<String, String> m = getCloudConfigs(props.getCloud());
        if (m.isEmpty()) {
            logger.info("all necessary cloud configs are not set. will check onprem properties");
            m = getOnPremConfigs(props.getOnPrem());
        }
        builder.putAll(m);

        Map<String, String> resourceAttributes = props.getGlobalAttributes();
        updateResourceAttribute(resourceAttributes, globalAttributeUpdates);
        builder.put("otel.resource.attributes", convertResourceAttributes(resourceAttributes));
        return builder.build();
    }

    static Map<String, String> getExporters(boolean isDebugLogging) {
        String exporters = !isDebugLogging ? "otlp" : "otlp,logging";
        Map<String, String> m = new HashMap<>();
        m.put("otel.traces.exporter", exporters);
        m.put("otel.metrics.exporter", exporters);
        m.put("otel.logs.exporter", exporters);
        return m;
    }

    static Map<String, String> getCloudConfigs(GrafanaProps.CloudProperties cloudProps) {
        Map<String, String> m = new HashMap<>();
        String verifyMsg = "will not attempt to send data to grafana.com: %s is not set.";
        try {
            Verify.verify(StringUtils.isBlank(cloudProps.getApiKey()), verifyMsg, "apiKey");
            Verify.verify(cloudProps.getInstanceId() == 0, verifyMsg, "instanceId");
            Verify.verify(StringUtils.isBlank(cloudProps.getZone()), verifyMsg, "zone");
        } catch (VerifyException ve) {
            logger.info("to send data to grafana.com cloud stack the apiKey, instanceId and zone must all be set {}",
                    verifyMsg);
            return m;
        }
        m.put("otel.exporter.otlp.endpoint",
                String.format("https://otlp-gateway-%s.grafana.net/otlp", cloudProps.getZone()));
        m.put("otel.exporter.otlp.protocol", cloudProps.getProtocol());
        m.put("otel.exporter.otlp.headers", getOtlpHeaders(cloudProps.getInstanceId(), cloudProps.getApiKey()));
        return m;
    }

    static Map<String, String> getOnPremConfigs(GrafanaProps.OnPremProperties onpremProps) {
        Map<String, String> m = new HashMap<>();
        if (StringUtils.isNotBlank(onpremProps.getEndpoint())) {
            m.put("otel.exporter.otlp.endpoint", onpremProps.getEndpoint());
        }
        if (StringUtils.isNotBlank(onpremProps.getProtocol())) {
            m.put("otel.exporter.otlp.protocol", onpremProps.getProtocol());
        }
        return m;
    }

    static String getOtlpHeaders(int instanceId, String apiKey) {
        String userPass = String.format("%d:%s", instanceId, apiKey);
        return String.format(
                "Authorization=Basic %s", Base64.getEncoder().encodeToString(userPass.getBytes()));
    }

    static void updateResourceAttribute(Map<String, String> resourceAttributes, Map<String, List<String>> updates) {
        Set<Map.Entry<String, List<String>>> entries = updates.entrySet();
        for (Map.Entry<String, List<String>> e : entries) {
            if (!resourceAttributes.containsKey(e.getKey())) {
                for (String value : e.getValue()) {
                    if (StringUtils.isNotBlank(value)) {
                        resourceAttributes.put(e.getKey(), value);
                        break;
                    }
                }
            }
        }
    }

    static String convertResourceAttributes(Map<String, String> resourceAttributes) {
        return resourceAttributes.entrySet().stream()
                .map(e -> String.format("%s=%s", e.getKey(), e.getValue()))
                .collect(Collectors.joining(","));
    }
}
