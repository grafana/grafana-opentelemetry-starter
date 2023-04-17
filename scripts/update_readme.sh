#!/usr/bin/env bash

sed -i 's/# Documentation/# Properties/g' README.md
sed -i --regexp-extended 's/## `(private )?(final )?([^ ]+ )(String> )?([a-zA-Z]+) ?.*`/## \5/g' README.md
