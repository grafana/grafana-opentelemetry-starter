# Changelog

## Version 1.3.0. (2023-06-26)

* Logger is configured automatically for Logback and Log4j2 - no need to add any configuration to your application (if you have configured the OpenTelemetry logger already, it will be used)

## Version 1.2.0. (2023-06-06)

* Set the base time unit to "seconds" - which ensures future compatibility with upcoming versions of the Grafana Agent 
* Support thread name for logging 

## Version 1.1.0. (2023-06-02)

* Add support for log4j
* Bugfix: starter can now be used with maven

## Version 1.0.1. (2023-05-23)

### Enhancements

* Include open-telemetry resources [(#14)](https://github.com/grafana/grafana-opentelemetry-starter/pull/14)

## Version 1.0.0. (2023-04-24)

* Initial release
