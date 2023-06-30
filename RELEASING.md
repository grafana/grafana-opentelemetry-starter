# Releasing

## Publish Release via Github Workflow

### Prerequisites

Install github's command line tool, `gh`.  

If you are on a mac, you can install the tool with [Homebrew](https://brew.sh/).

```
> brew install gh
```

Once the tool is installed, you will need to authenticate with github.  To do so run:

```
> gh auth login
```
You will be asked several questions regarding how you want to log on (i.e. what account and protocol).

Once authenticated you can continue to the next section.

### Prepare for Release

Create/switch to a new branch off of `main`.

```
git checkout -b Update_for_new_release
```

From the project root, run the following command to update the repo with the new version (ex. 1.0.0)
```
> ./scripts/release.sh "<VERSION>" 
```
Also update the repo's CHANGELOG with details about the release. Then commit/push the changes and open a PR. 
Merge the PR once approved.

### Tag and Publish New Release

From the repo's `main` branch `git pull` the new changes.  Then run the following
[command](https://cli.github.com/manual/gh_release_create) to create a tag (if one does not already exist 
for the version) and publish the release to the 
[Sonatype repository](https://s01.oss.sonatype.org/content/groups/staging/com/grafana/grafana-opentelemetry-starter/). Remember to update the version before running.

```
> gh release create <VERSION>
```

You will be asked several questions regarding the release.  You can leave the release notes blank
or include details from the CHANGELOG.

You can review the build/publish workflow script in `/.github/workflows/ci.yml` and review the pipeline's progress in the
repo's [action](https://github.com/grafana/grafana-opentelemetry-starter/actions) page once the command is executed.

## Publish Release Manually

### Prerequisites

- Create a sonatype account and create an [issue](https://issues.sonatype.org/browse/OSSRH-90665) to get approved by one of the maintainers
- Get a GPG key - https://central.sonatype.org/publish/requirements/gpg/#credentials
- Push the GPG key to ubuntu - https://central.sonatype.org/publish/requirements/gpg/#credentials
- Export the secret key with `gpg --armor --export-secret-keys <your.name>@grafana.com > ~/.gnupg/grafana-secret-key.txt`, should look like this:

```
-----BEGIN PGP PRIVATE KEY BLOCK-----

<multile lines>

-----END PGP PRIVATE KEY BLOCK-----
```

### Publish to Nexus Repository

#### Prepare for Release

Create/switch to a new branch off of `main`.

```
> git checkout -b Update_for_new_release
```

From the project's root, run the following command to update the repo with the new version (ex. 1.0.0)
```
> ./scripts/release.sh "<VERSION>" 
```

Also update the repo's CHANGELOG with details about the release. Then commit/push the changes and open a PR. 
Merge the PR once approved.

#### Tag and Publish New Release

From the repo's `main` branch, `git pull` the new changes. Then export the following environment variables 
and run the gradle command to publish.

```shell
export OSSRH_USERNAME=<sonatype user>
export OSSRH_PASSWORD=<sonatype password>
export "SIGNING_KEY=$(cat ~/.gnupg/grafana-secret-key.txt)"
export SIGNING_PASSWORD=<passphrase of gpg key>

./gradlew publishToSonatype closeAndReleaseSonatypeStagingRepository
```

### Publish to Local Maven Repository

From the project's root directory, run the following to export the environment variable and publish a release 
to your local `~/.m2` repository.

```shell
./gradlew publishToMavenLocal
```
