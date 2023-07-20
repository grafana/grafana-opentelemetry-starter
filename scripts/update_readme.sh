#!/usr/bin/env bash

sed -i 's/# Documentation/### Properties/g' README.generated
sed -i --regexp-extended 's/## `(private )?(final )?([^ ]+ )(String> )?([a-zA-Z]+) ?.*`/#### \5/g' README.generated
sed -i 's/endpoint/grafana.otlp.onprem.endpoint/g' README.generated
sed -i 's/protocol/grafana.otlp.onprem.protocol/g' README.generated
sed -i 's/zone/grafana.otlp.cloud.zone/g' README.generated
sed -i 's/apiKey/grafana.otlp.cloud.apiKey/g' README.generated
sed -i 's/instanceId/grafana.otlp.cloud.instanceId/g' README.generated
sed -i 's/debugLogging/grafana.otlp.debugLogging/g' README.generated
sed -i 's/enabled/grafana.otlp.enabled/g' README.generated
sed -i 's/globalAttributes/grafana.otlp.globalAttributes/g' README.generated
