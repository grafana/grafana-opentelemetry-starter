/*
 * Copyright Grafana Labs
 * SPDX-License-Identifier: Apache-2.0
 */

package com.grafana.opentelemetry.resources;

import com.grafana.opentelemetry.DistributionVersion;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.ResourceAttributes;

public final class DistributionResource {
  public static final AttributeKey<String> DISTRIBUTION_NAME =
      AttributeKey.stringKey("telemetry.distro.name");
  public static final AttributeKey<String> DISTRIBUTION_VERSION =
      AttributeKey.stringKey("telemetry.distro.version");

  private static final Resource INSTANCE = buildResource();

  private DistributionResource() {}

  public static Resource get() {
    return INSTANCE;
  }

  static Resource buildResource() {
    return Resource.create(
        Attributes.of(
            DISTRIBUTION_NAME,
            "grafana-opentelemetry-starter",
            DISTRIBUTION_VERSION,
            DistributionVersion.VERSION),
        ResourceAttributes.SCHEMA_URL);
  }
}
