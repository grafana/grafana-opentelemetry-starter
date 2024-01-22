/*
 * Copyright Grafana Labs
 * SPDX-License-Identifier: Apache-2.0
 */

package com.grafana.opentelemetry.resources;

import io.micrometer.common.util.StringUtils;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.ResourceAttributes;
import java.util.Map;
import java.util.UUID;

/**
 * In the spirit of <a
 * href="https://docs.google.com/document/d/1BenPf9vsZHCf4JpHWGQBydKZAA4XdH38wuMD7JQnz9A/edit?pli=1#heading=h.lyyq7xqgpsj9">this
 * proposal</a>
 */
public class ServiceInstanceIdResource {

  public static final String RESOURCE_ATTRIBUTES = "otel.resource.attributes";
  public static final String RANDOM_INSTANCE_ID = UUID.randomUUID().toString();

  private ServiceInstanceIdResource() {}

  public static Resource getResource(ConfigProperties config) {
    Map<String, String> resourceAttributes = config.getMap(RESOURCE_ATTRIBUTES);
    String k8s = k8sServiceInstanceId(resourceAttributes);
    String value;
    value = k8s != null ? k8s : RANDOM_INSTANCE_ID;
    return Resource.create(
        Attributes.of(ResourceAttributes.SERVICE_INSTANCE_ID, value),
        ResourceAttributes.SCHEMA_URL);
  }

  private static String k8sServiceInstanceId(Map<String, String> resource) {
    String podName = resource.get(ResourceAttributes.K8S_POD_NAME.getKey());
    String containerName = resource.get(ResourceAttributes.K8S_CONTAINER_NAME.getKey());
    return StringUtils.isNotBlank(podName) && StringUtils.isNotBlank(containerName)
        ? podName + "/" + containerName
        : null;
  }
}
