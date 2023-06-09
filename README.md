# Overview

The grafana-opentelemetry-starter makes it easy to use Metrics, Traces, and Logs with OpenTelemetry
in Grafana Cloud or with Grafana Agent (for Grafana Cloud or Grafana OSS stack).

# Compatibility

| Spring Boot Version | Java Version | Recommended Setup                                                                        |
|---------------------|--------------|------------------------------------------------------------------------------------------|
| 3.1+                | 17+          | Use this starter                                                                         |
| 3.0.4+              | 17+          | Use this starter in version 1.0.0 (only works with gradle)                               |
| 2.x                 | 8+           | Use the [Java Agent](https://grafana.com/docs/opentelemetry/instrumentation/java-agent/) |

# Getting Started

Add the following dependency to your `build.gradle`

```groovy
implementation 'com.grafana:grafana-opentelemetry-starter:1.2.0'
```

... or `pom.xml`

```xml
<dependency>
    <groupId>com.grafana</groupId>
    <artifactId>grafana-opentelemetry-starter</artifactId>
    <version>1.2.0</version>
</dependency>
```

## Logging

To implement logging, register an appender for one of the following frameworks.

### Logback

To register a logback appender, create a new logback-spring.xml (or logback.xml) file under your project’s 
`main/resources` directory and copy the following xml.

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
            class="io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender"
            captureExperimentalAttributes="true">
  </appender>

  <root level="INFO">
    <appender-ref ref="console"/>
    <appender-ref ref="OpenTelemetry"/>
  </root>
</configuration>
```

### Log4j2

To register a log4j2 appender, create a new log4j2.xml file under your project’s 
`main/resources` directory and copy the following xml.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration packages="io.opentelemetry.instrumentation.log4j.appender.v2_17">
  <Appenders>
    <Console name="Console" target="SYSTEM_OUT">
      <PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n"/>
    </Console>
    <OpenTelemetry name="OpenTelemetryAppender" captureExperimentalAttributes="true"/>
  </Appenders>
  <Loggers>
    <Root level="info">
      <AppenderRef ref="OpenTelemetryAppender"/>
      <AppenderRef ref="Console"/>
    </Root>
  </Loggers>
</Configuration>
```

## Configuration

Finally, configure your application.yaml or application.properties either for Grafana Cloud or Grafana Agent.

### Grafana Cloud

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

### Grafana Agent

application.yaml:

```yaml
spring:
  application:
    name: demo-app
```

- [How to configure the Grafana Agent](https://grafana.com/docs/opentelemetry/instrumentation/grafana-agent/)
- Refer to the [Properties section](#properties) for details about configuration properties

If you have a changed the configuration of the Grafana Agent,
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

## Grafana Dashboard

Once you've started your application, you can use this [Spring Boot Dashboard](https://grafana.com/grafana/dashboards/18887)

# Reference

- All configuration properties are described in the [reference](#properties).
- The `grafana.otlp.cloud` and `grafana.otlp.onprem` properties are mutually exclusive.
- As usual in Spring Boot, you can use environment variables to supply some of the properties, which is especially
  useful for secrets, e.g. `GRAFANA_OTLP_CLOUD_API_KEY` instead of `grafana.otlp.cloud.apiKey`.
- In addition, you can use all system properties or environment variables from the
  [SDK auto-configuration](https://github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions/autoconfigure) -
  which will take precedence.

## Troubleshooting

When you start the application, you will also get a log output of the configuration properties as they are translated into SDK properties.

For example, if you set the `spring.application.name` in `application.yaml`,
you will get the following log output:

```
11:53:07.724 [main] INFO  c.g.o.OpenTelemetryConfig - using config properties: {otel.exporter.otlp.endpoint=https://otlp-gateway-prod-eu-west-0.grafana.net/otlp, otel.logs.exporter=otlp, otel.traces.exporter=otlp, otel.exporter.otlp.headers=Authorization=Basic NTUz..., otel.exporter.otlp.protocol=http/protobuf, otel.resource.attributes=service.name=demo-app, otel.metrics.exporter=otlp}
```

(The `otel.exporter.otlp.headers` field is abbreviated for security reasons.)

If you still don't see your logs, traces and metrics in Grafana, even though the configuration looks good, 
you can turn on [debug loggong](#grafanaotlpdebuglogging) to what data the application is emitting.

## Properties

### grafana.otlp.globalAttributes

Adds global (resource) attributes to metrics, traces and logs.

For example, you can add `service.version` to make it easier to see if a new version of the application is causing a problem.



The attributes `service.name`, `service.version`, and `service.instance.id` are automatically detected as outlined below.



For `service.name` the order of precedence is: <ol> <li>environment variable OTEL_SERVICE_NAME</li> <li>environment variable OTEL_RESOURCE_ATTRIBUTES</li> <li>Manually set service_name in grafana.otlp.grafana.otlp.globalAttributes</li> <li>spring.application.name" in application.properties</li> <li>'Implementation-Title' in jar's MANIFEST.MF</li> </ol>

The following block can be added to build.gradle to set the application name and version in the jar's MANIFEST.MF: <pre> bootJar { manifest { attributes('Implementation-Title': 'Demo Application', 'Implementation-Version': version) } } </pre> The `service.instance.id` attribute will be set if any of the following return a value. The list is in order of precedence. <ol> <li>InetAddress.getLocalHost().getHostName()</li> <li>environment variable HOSTNAME</li> <li>environment variable HOST</li> </ol>

### grafana.otlp.debugLogging

Log all metrics, traces, and logs that are created for debugging purposes (in addition to sending them to the backend via OTLP).

This will also send metrics and traces to Loki as an unintended side effect.

### grafana.otlp.cloud.zone

The Zone can be found when you click on "Details" in the "Grafana" section on grafana.com.

Use `onprem.grafana.otlp.onprem.endpoint` instead of `grafana.otlp.cloud.zone` when using the Grafana Agent.

### grafana.otlp.cloud.instanceId

The Instance ID can be found when you click on "Details" in the "Grafana" section on grafana.com.

Leave `grafana.otlp.cloud.instanceId` empty when using the Grafana Agent.

### grafana.otlp.cloud.apiKey

Create an API key under "Security" / "API Keys" (left side navigation tree) on grafana.com. The role should be "MetricsPublisher"

Leave `grafana.otlp.cloud.apiKey` empty when using the Grafana Agent.

### grafana.otlp.onprem.endpoint

The grafana.otlp.onprem.endpoint of the Grafana Agent.

You do not need to set an `grafana.otlp.onprem.endpoint` value if your Grafana Agent is running locally with the default gRPC grafana.otlp.onprem.endpoint (localhost:4317).

Use `cloud.grafana.otlp.cloud.zone` instead of `grafana.otlp.onprem.endpoint` when using the Grafana Cloud.

### grafana.otlp.onprem.protocol

The grafana.otlp.onprem.protocol used to send OTLP data. Can be either `http/protobuf` or `grpc` (default).
