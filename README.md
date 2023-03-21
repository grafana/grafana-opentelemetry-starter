# Warning
This is only an experiment so far.

# Installation

build.gradle:
```groovy
implementation 'com.grafana:grafana-opentelemetry-starter:0.0.1-SNAPSHOT'
```

application.yaml

```yaml
spring:
  application:
    name: demo-app

grafana:
  otlp:
    endpoint: https://otlp-gateway-<Grafana region>.grafana.net/otlp
    instanceID: <Grafana Instance ID>
    apiKey: <Grafana API key>
    consoleLogging: true
    resourceAttributes:
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
