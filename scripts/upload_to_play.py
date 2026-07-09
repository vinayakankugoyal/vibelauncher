#!/usr/bin/env python3
"""Upload an .aab to a Google Play track.

Usage: upload_to_play.py <bundle.aab> <track>

Authenticates via Application Default Credentials: point
GOOGLE_APPLICATION_CREDENTIALS at either a service-account JSON key or a
workload-identity-federation credential config. Uses only Google's official
client libraries.
"""

import sys

import google.auth
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload

PACKAGE_NAME = "com.pizza.vibelauncher"


def main() -> None:
    if len(sys.argv) != 3:
        sys.exit(f"usage: {sys.argv[0]} <bundle.aab> <track>")
    aab_path, track = sys.argv[1], sys.argv[2]

    credentials, _ = google.auth.default(
        scopes=["https://www.googleapis.com/auth/androidpublisher"]
    )
    edits = build("androidpublisher", "v3", credentials=credentials).edits()

    edit_id = edits.insert(packageName=PACKAGE_NAME).execute()["id"]
    print(f"created edit {edit_id}")

    bundle = edits.bundles().upload(
        packageName=PACKAGE_NAME,
        editId=edit_id,
        media_body=MediaFileUpload(aab_path, mimetype="application/octet-stream"),
    ).execute()
    version_code = bundle["versionCode"]
    print(f"uploaded bundle versionCode={version_code}")

    edits.tracks().update(
        packageName=PACKAGE_NAME,
        editId=edit_id,
        track=track,
        body={"releases": [{"versionCodes": [str(version_code)], "status": "completed"}]},
    ).execute()
    print(f"assigned versionCode {version_code} to track '{track}'")

    edits.commit(packageName=PACKAGE_NAME, editId=edit_id).execute()
    print("edit committed - release is live on the track")


if __name__ == "__main__":
    main()
