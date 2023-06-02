# Overview

The grafana-opentelemetry-starter makes it easy to use Metrics, Traces, and Logs with OpenTelemetry
in Grafana Cloud or with Grafana Agent (for Grafana Cloud or Grafana OSS stack).

# Compatibility

| Spring Boot Version | Java Version | Recommended Setup                                                                        |
|---------------------|--------------|------------------------------------------------------------------------------------------|
| 3.1+                | 17+          | Use this starter                                                                         |
| 3.0.4+              | 17+          | Use this starter in version 1.0.0 (only works with gradle)                               |
| 2.x                 | 8+           | Use the [Java Agent](https://grafana.com/docs/opentelemetry/instrumentation/java-agent/) |

# Installation

Add the following dependency to your `build.gradle`

```groovy
implementation 'com.grafana:grafana-opentelemetry-starter:1.0.1'
```

... or `pom.xml`

```xml
<dependency>
    <groupId>com.grafana</groupId>
    <artifactId>grafana-opentelemetry-starter</artifactId>
    <version>1.0.1</version>
</dependency>
```

The next step depends on your logging framework - logback and log4j2 are supported:

## Logback

If you use logback, register the OpenTelemetry logback appender in `logback-spring.xml` (or `logback.xml`):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
  <appender name="console" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
      <pattern>
        %d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n
      </pattern>
    </encoder>
  </appender>
  <appender name="OpenTelemetry"
            class="io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender">
  </appender>

  <root level="INFO">
    <appender-ref ref="console"/>
    <appender-ref ref="OpenTelemetry"/>
  </root>
</configuration>
```

## Log4j2

If you use log4j2, register the OpenTelemetry logback appender in `log4j2.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration packages="io.opentelemetry.instrumentation.log4j.appender.v2_17">
  <Appenders>
    <Console name="Console" target="SYSTEM_OUT">
      <PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n"/>
    </Console>
    <OpenTelemetry name="OpenTelemetryAppender"/>
  </Appenders>
  <Loggers>
    <Root level="info">
      <AppenderRef ref="OpenTelemetryAppender"/>
      <AppenderRef ref="Console"/>
    </Root>
  </Loggers>
</Configuration>
```

Finally, configure your application.yaml or application.properties either for Grafana Cloud or Grafana Agent.

## Grafana Cloud

> ⚠️ Please use the Grafana Agent configuration for production use cases.

application.yaml:

```yaml
spring:
  application:
    name: demo-app

grafana:
  otlp:
    cloud:
      zone: <Grafana Zone>
      instanceId: <Grafana Instance ID>
      apiKey: <Grafana API key>
```

## Grafana Agent

application.yaml:

```yaml
spring:
  application:
    name: demo-app
```

- [How to configure the Grafana Agent](https://grafana.com/docs/opentelemetry/instrumentation/grafana-agent/)
- [Reference](#properties) of all configuration properties

If you have a changed the configuration of the grafana agent,
you can specify the endpoint and protocol.
This example uses the default values - it is equivalent to the example above:

```yaml
spring:
  application:
    name: demo-app
grafana:
  otlp:
    onprem:
      endpoint: http://localhost:4317
      protocol: grpc
```

# Configuration

- All configuration properties are described in the [reference](#properties).
- The `grafana.otlp.cloud` and `grafana.otlp.onprem` properties are mutually exclusive.
- As usual in Spring Boot, you can use environment variables to supply some of the properties, which is especially
  useful for secrets, e.g. `GRAFANA_OTLP_CLOUD_API_KEY` instead of `grafana.otlp.cloud.apiKey`.
- In addition, you can use all system properties or environment variables from the
  [SDK auto-configuration](https://github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions/autoconfigure) -
  which will take precedence.

When you start the application, you will also get a log output of the configuration properties as they are translated into SDK properties.

For example, if you set the `spring.application.name` in `application.yaml`,
you will get the following log output:

```
11:53:07.724 [main] INFO  c.g.o.OpenTelemetryConfig - using config properties: {otel.exporter.otlp.endpoint=https://otlp-gateway-prod-eu-west-0.grafana.net/otlp, otel.logs.exporter=otlp, otel.traces.exporter=otlp, otel.exporter.otlp.headers=Authorization=Basic NTUz..., otel.exporter.otlp.protocol=http/protobuf, otel.resource.attributes=service.name=demo-app, otel.metrics.exporter=otlp}
```

(The `otel.exporter.otlp.headers` field is abbreviated for security reasons.)

# Properties

## grafana.otlp.globalAttributes

Adds global (resource) attributes to metrics, traces and logs.

For example, you can add `service.version` to make it easier to see if a new version of the application is causing a problem.

The attributes `service.name`, `service.version`, and `service.instance.id` are automatically detected as explained below, but if you set the value manually, it will be respected.

"spring.application.name" in application.properties will be translated to `service.name`.

You can also add the application name and version to MANIFEST.MF, where they will be copied to `service.name` and `service.version` respectively.

In gradle, the application name and version can be set as follows: <pre> bootJar { manifest { attributes('Implementation-Title': 'Demo Application', 'Implementation-Version': version) } } </pre> The environment variables HOST or HOSTNAME will be translated to `service.instance.id`.

## grafana.otlp.debugLogging

Log all metrics, traces, and logs that are created for debugging purposes (in addition to sending them to the backend via OTLP).

This will also send metrics and traces to Loki as an unintended side effect.

## grafana.otlp.cloud.zone

The Zone can be found when you click on "Details" in the "Grafana" section on grafana.com.

Use `onprem.endpoint` instead of `zone` when using the Grafana Agent.

## grafana.otlp.cloud.instanceId

The Instance ID can be found when you click on "Details" in the "Grafana" section on grafana.com.

Leave `instanceId` empty when using the Grafana Agent.

## grafana.otlp.cloud.apiKey

Create an API key under "Security" / "API Keys" (left side navigation tree) on grafana.com. The role should be "MetricsPublisher"

Leave `apiKey` empty when using the Grafana Agent.

## grafana.otlp.onprem.endpoint

The endpoint of the Grafana Agent.

You do not need to set an `endpoint` value if your Grafana Agent is running locally with the default gRPC endpoint (localhost:4317).

Use `cloud.zone` instead of `endpoint` when using the Grafana Cloud.

## grafana.otlp.onprem.protocol

The protocol used to send OTLP data. Can be either `http/protobuf` or `grpc` (default).
