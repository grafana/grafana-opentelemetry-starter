take a look at https://github.com/grafana/JPProf, which is similar

# Releasing

- get a GPG key - https://central.sonatype.org/publish/requirements/gpg/#credentials
- push the GPG key to ubuntu - https://central.sonatype.org/publish/requirements/gpg/#credentials
- add the properties below to your ~/.gradle/gradle.properties and fill in your sonatype credentials

```properties
ossrhUsername=<user>
ossrhPassword=<password>
```

Release process is WIP

