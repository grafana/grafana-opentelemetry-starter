# Releasing

To cut a new release:

- `./scripts/release.sh "1.0"` (or whatever the new version is)
- Open a PR
- Once the PR is merged, push a git tag with the version number

## Setup

take a look at https://github.com/grafana/JPProf, which is similar
                                              
- create a sonatype account and create an [issue](https://issues.sonatype.org/browse/OSSRH-90665) to get approved by one of the maintainers 
- get a GPG key - https://central.sonatype.org/publish/requirements/gpg/#credentials
- push the GPG key to ubuntu - https://central.sonatype.org/publish/requirements/gpg/#credentials
- Export the secret key with `gpg --armor --export-secret-keys <your.name>@grafana.com > ~/.gnupg/grafana-secret-key.txt`, should look like this:

```
-----BEGIN PGP PRIVATE KEY BLOCK-----

<multile lines>

-----END PGP PRIVATE KEY BLOCK-----
```

## Manually Releasing

export the following env vars - which are also stored as secrets in github actions

```shell
export OSSRH_USERNAME=<sonatype user>
export OSSRH_PASSWORD=<sonatype password>
export "SIGNING_KEY=$(cat ~/.gnupg/grafana-secret-key.txt)"
export SIGNING_PASSWORD=<passphrase of gpg key>

`./gradlew publishToSonatype closeAndReleaseSonatypeStagingRepository`
```

or `./gradlew publishToMavenLocal` to deploy locally only.
