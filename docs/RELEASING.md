# Releasing

Releases are built and uploaded to the Play Store **closed testing** track
automatically by the [`deploy.yml`](../.github/workflows/deploy.yml) GitHub
Actions workflow.

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

The workflow needs five repository secrets
(**Settings → Secrets and variables → Actions**):

| Secret | Contents |
| --- | --- |
| `RELEASE_KEYSTORE_BASE64` | Your **existing** upload keystore, base64 encoded: `base64 -w0 upload-keystore.jks` |
| `RELEASE_KEYSTORE_PASSWORD` | Keystore password |
| `RELEASE_KEY_ALIAS` | Key alias inside the keystore |
| `RELEASE_KEY_PASSWORD` | Key password |
| `PLAY_SERVICE_ACCOUNT_JSON` | Google Cloud service account JSON key (see below) |

> **Important:** the keystore must be the same upload keystore you have been
> signing releases with until now (the `.jks`/`.keystore` file on your
> laptop). Play only accepts uploads signed with the upload key it already
> knows for this app — a freshly generated key will be rejected. You can
> check which key Play expects under **Play Console → Setup → App signing →
> Upload key certificate**, and compare it to your keystore with
> `keytool -list -v -keystore upload-keystore.jks`. If the keystore is ever
> lost, Play App Signing lets you request an upload key reset from that same
> page.

### Creating the service account

1. In [Google Cloud Console](https://console.cloud.google.com/), pick (or
   create) a project and enable the **Google Play Android Developer API**.
2. Create a service account (IAM & Admin → Service Accounts), then create a
   **JSON key** for it and download it. That file's contents go into the
   `PLAY_SERVICE_ACCOUNT_JSON` secret.
3. In [Play Console](https://play.google.com/console), go to **Users and
   permissions → Invite new user**, enter the service account's email address,
   and grant it access to this app with the **Release to testing tracks**
   permission (or the "Release manager" role).

### Notes

- The `track: alpha` in the workflow targets the default closed testing
  track. If you use a custom closed track, replace `alpha` with the track's
  name as shown in Play Console.
- The very first upload of an app can't be done through the API — that has
  already happened for this app, so tag away.
