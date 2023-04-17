**Status:** Experimental

# Installation

build.gradle:
```groovy
implementation 'com.grafana:grafana-opentelemetry-starter:0.0.6'
```

application.yaml

```yaml
spring:
  application:
    name: demo-app

grafana:
  otlp:
    endpoint: https://otlp-gateway-<Grafana Zone>.grafana.net/otlp
    instanceId: <Grafana Instance ID>
    apiKey: <Grafana API key>
    debug: true
    globalAttributes:
      k8s.pod.name: nevla
```

logback-spring.xml:

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

# Properties

## endpoint

The grafana cloud OTLP gateway endpoint in the form of `https://otlp-gateway-<Zone>.grafana.net/otlp`

The Zone can be found when you click on "Details" in the "Grafana" section on grafana.com.

## protocol

The protocol used to send OTLP data. Can be either `http/protobuf` (which is the default) or `grpc`.

## instanceId

The Instance ID can be found when you click on "Details" in the "Grafana" section on grafana.com.

Leave this field empty when using the Grafana OSS stack.

## apiKey

Create an API key under "Security" / "API Keys" (left side navigation tree) on grafana.com. The role should be "MetricsPublisher"

Leave this field empty when using the Grafana OSS stack.

## globalAttributes

Adds global (resource) attributes to metrics, traces and logs.

For example, you can add `service.version` to make it easier to see if a new version of the application is causing a problem.

The attributes `service.name`, `service.version`, and `service.instance.id` are automatically detected as explained below, but if you set the value manually, it will be respected.

"spring.application.name" in application.properties will be translated to `service.name`.

You can also add the application name and version to MANIFEST.MF, where they will be copied to `service.name` and `service.version` respectively.

In gradle, the application name and version can be set as follows: <pre> bootJar { manifest { attributes('Implementation-Title': 'Demo Application', 'Implementation-Version': version) } } </pre> The environment variables HOST or HOSTNAME will be translated to `service.instance.id`.

## debugLogging

Log all metrics, traces, and logs that are created for debugging purposes (in addition to sending them to the backend via OTLP).

This will also send metrics and traces to Loki as an unintended side effect.
