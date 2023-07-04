# Changes to GrafanaProperties

The properties in README.md are generated from GrafanaProperties.java.

- Go to https://delight-im.github.io/Javadoc-to-Markdown/
- Copy the content of GrafanaProperties.java
- Paste the result into README.generated (create the file if it doesn't exist)
- `scripts/update_readme.sh`
- Paste README.generated into README.md starting with `# Properties`

# Update README.md

Please create a PR to the [docs page](https://github.com/grafana/opentelemetry-docs/blob/main/docs/sources/instrumentation/spring-starter.md)
when there's a significant change to the README.md.
The docs page contains everything from the README.md, except the properties section.
The content can be copied over - but some links become relative links and need to be fixed manually - this hasn't been
automated yet.
