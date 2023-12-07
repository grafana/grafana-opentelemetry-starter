# Changelog
                  
## Version 1.4.0 (2023-12-07)

* This version supports Spring Boot 3.2.0
* Add resource attributes `telemetry.distro.name` = `grafana-opentelemetry-starter` and `telemetry.distro.version` = `1.4.0`
  
## Version 1.3.2 (2023-07-19)
                                       
* You can now disable the starter by setting `spring.opentelemetry.enabled=false` in your application.yaml or application.properties

## Version 1.3.1 (2023-06-29)
                
* Fix histogram bucket boundaries - it was a lower bound of 1 - regardless of the unit (and 1s is too large for server response times)
* enable histograms for "http.server.requests" 
* Logger is configured automatically for Logback and Log4j2 - no need to add any configuration to your application (if you have configured the OpenTelemetry logger already, it will be used)

## Version 1.3.0 (2023-06-26)
                         
* Broken for log4j - use 1.3.1 instead

## Version 1.2.0 (2023-06-06)

* Set the base time unit to "seconds" - which ensures future compatibility with upcoming versions of the Grafana Agent 
* Support thread name for logging 

## Version 1.1.0 (2023-06-02)

* Add support for log4j
* Bugfix: starter can now be used with maven

## Version 1.0.1 (2023-05-23)

### Enhancements

* Include open-telemetry resources [(#14)](https://github.com/grafana/grafana-opentelemetry-starter/pull/14)

## Version 1.0.0 (2023-04-24)

* Initial release
