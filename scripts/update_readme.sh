#!/usr/bin/env bash

sed -i 's/# Documentation/# Properties/g' README.md
sed -i --regexp-extended 's/## `(private )?(final )?([^ ]+ )(String> )?([a-zA-Z]+) ?.*`/## \5/g' README.md
sed -i 's/## endpoint/## grafana.otlp.onprem.endpoint/g' README.md
sed -i 's/## zone/## grafana.otlp.cloud.zone/g' README.md
sed -i 's/## apiKey/## grafana.otlp.cloud.apiKey/g' README.md
sed -i 's/## instanceId/## grafana.otlp.cloud.instanceId/g' README.md
sed -i 's/## protocol/## grafana.otlp.protocol/g' README.md
sed -i 's/## debugLogging/## grafana.otlp.debugLogging/g' README.md
sed -i 's/## globalAttributes/## grafana.otlp.globalAttributes/g' README.md
