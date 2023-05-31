# Changelog

## unreleased
                                                         
* TODO release 2.0.0 - describe changes better
* gRPC not supported anymore
* env vars not supported anymore, because they will only affect the trace and log exporter
* log4j is now supported

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
