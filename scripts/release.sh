#!/usr/bin/env bash

newVersion=$1
if [ -z "$newVersion" ]; then
  echo "new version is missing"
  exit 1
fi

oldVersion=$(grep -oP "(?<=grafanaOtelStarterVersion=)(.*)" gradle.properties)
sed -i "s/$oldVersion/$newVersion/g" README.md
sed -i "s/$oldVersion/$newVersion/g" gradle.properties
