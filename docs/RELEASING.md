# Releasing

Releases are built and uploaded to the Play Store **closed testing** track
automatically by the [`deploy.yml`](../.github/workflows/deploy.yml) GitHub
Actions workflow. The upload itself is done by our own script,
[`scripts/upload_to_play.py`](../scripts/upload_to_play.py), which uses only
Google's official client libraries — no third-party GitHub action ever sees
the signing key or Play credentials. The only actions used are GitHub's own
(`actions/checkout`, `actions/setup-java`).

## Releasing a new version

Tag the commit you want to ship and push the tag — that's the whole release:

```sh
git tag v1.0.11
git push origin v1.0.11
```

The version comes from the tag, so there is no gradle file to edit:
`v1.0.11` becomes `versionName 1.0.11` and
`versionCode 10011` (`major*10000 + minor*100 + patch`), which is always
higher than the previous release as long as the tag number goes up.

CI builds a signed `.aab` and uploads it to closed testing. You can also
trigger the workflow manually from the Actions tab ("Run workflow"); it will
use the most recent `v*` tag. Promoting a build from closed testing to
production stays a manual step in Play Console.

## One-time setup

### 1. Create the Play service account

1. In [Google Cloud Console](https://console.cloud.google.com/), pick (or
   create) a project and enable the **Google Play Android Developer API**.
2. Create a service account (IAM & Admin → Service Accounts), then create a
   **JSON key** for it and download it.
3. In [Play Console](https://play.google.com/console), go to **Users and
   permissions → Invite new user**, enter the service account's email address,
   and grant it access to this app with the **Release to testing tracks**
   permission only — then even a leaked credential can't touch production.

### 2. Create the repository secrets

Run these on the machine that has your upload keystore (requires the
[GitHub CLI](https://cli.github.com/); or add them by hand under
**Settings → Secrets and variables → Actions**):

```sh
cd vibelauncher

gh secret set RELEASE_KEYSTORE_BASE64 --body "$(base64 -w0 /path/to/upload-keystore.jks)"
gh secret set RELEASE_KEYSTORE_PASSWORD    # prompts; paste the keystore password
gh secret set RELEASE_KEY_ALIAS            # prompts; paste the key alias
gh secret set RELEASE_KEY_PASSWORD         # prompts; paste the key password
gh secret set PLAY_SERVICE_ACCOUNT_JSON < /path/to/service-account-key.json
```

(On macOS use `base64 -i` instead of `base64 -w0`.)

> **Important:** the keystore must be the same upload keystore you have been
> signing releases with until now (the `.jks`/`.keystore` file on your
> laptop). Play only accepts uploads signed with the upload key it already
> knows for this app — a freshly generated key will be rejected. You can
> check which key Play expects under **Play Console → Setup → App signing →
> Upload key certificate**, and compare it to your keystore with
> `keytool -list -v -keystore upload-keystore.jks`. If the keystore is ever
> lost, Play App Signing lets you request an upload key reset from that same
> page.

## Security notes

- **Track selection**: the workflow uploads to the default closed testing
  track (`alpha`). If you use a custom closed track, change the track
  argument in the workflow's upload step to the track's name from Play
  Console.
- **Pinning actions**: `actions/checkout` and `actions/setup-java` are
  referenced by version tag and published by GitHub itself. For extra rigor,
  pin them to commit SHAs — look up the SHA with
  `gh api repos/actions/checkout/git/ref/tags/v4 --jq .object.sha` and
  replace the tag in the workflow with it.
- **Going keyless (optional)**: `scripts/upload_to_play.py` authenticates via
  Application Default Credentials, so `PLAY_SERVICE_ACCOUNT_JSON` can hold a
  Workload Identity Federation credential config instead of a long-lived JSON
  key. That requires setting up a workload identity pool and GitHub OIDC
  provider in Google Cloud, plus a step in the workflow that fetches the
  runner's OIDC token. Worth doing if the key ever becomes a concern; the
  script needs no changes.
- The very first upload of an app can't be done through the API — that has
  already happened for this app, so tag away.
