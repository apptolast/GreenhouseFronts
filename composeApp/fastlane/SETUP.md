# Fastlane Android setup

This document captures how the Android release pipeline is wired so the
project is reproducible on a fresh machine and on CI.

## Versioning model — tag-driven

We do **not** hand-bump `versionCode`/`versionName` in `build.gradle.kts` per
release. Instead:

- `versionName` is derived from a Git tag of the form `vMAJOR.MINOR.PATCH[-suffix]`.
- `versionCode` is `max(existing on all Play tracks) + 1`, queried by fastlane
  at lane runtime.
- Both values are injected into Gradle via `-PappVersionCode=N -PappVersionName=X`,
  so the repo never carries release version state.

The defaults in `composeApp/build.gradle.kts` (`versionCode = 3`, `versionName
= "0.2.0-dev"`) are only used by debug builds run from Android Studio.

## Local setup (macOS, run once per machine)

```bash
# 1. Install rbenv and a modern Ruby — see `.ruby-version` at the repo root.
brew install rbenv ruby-build
echo 'eval "$(rbenv init - zsh)"' >> ~/.zshrc
source ~/.zshrc
rbenv install $(cat .ruby-version)

# 2. Install bundler and gems.
gem install bundler -v "~> 2.5"
bundle install

# 3. Configure your local secrets.
cp composeApp/fastlane/.env.example composeApp/fastlane/.env
# Edit .env to point SUPPLY_JSON_KEY at the Play Console service account JSON.
```

## Local lanes

Run from `composeApp/`. fastlane only looks for the `fastlane/` folder in
cwd (it does not walk parent dirs the way Bundler does), so you must cd
into the module that owns the lanes. Bundler still finds the root `Gemfile`
walking up.

```bash
cd composeApp
bundle exec fastlane android version          # print local defaults
bundle exec fastlane android validate_play    # sanity-check Play credentials
bundle exec fastlane android build            # signed release AAB, no upload
bundle exec fastlane android internal         # build + upload to Internal as DRAFT
```

`internal` is the right lane while developing a feature: it lets you push a
build to your QA testers without consuming a release tag. The versionName is
auto-suffixed (`<default>-internal-<code>`) so it's clearly not a production
release. Override with `version_name:"0.3.0-rc1"` if you want a custom label.

## CI release flow (production)

1. Branch protections: `develop` and `main` are PR-only. Squash-merge.
2. Merge `feature/*` → `develop` via PR.
3. When `develop` is ready to ship, merge `develop` → `main` via PR.
4. On `main`, tag the release commit and push the tag:
   ```bash
   git checkout main && git pull
   git tag v0.3.0 -m "Release 0.3.0: alert notifications"
   git push --tags
   ```
5. The `Android Release` workflow (`.github/workflows/android-release.yml`)
   triggers, runs fastlane `release_from_tag`, and uploads the AAB to the
   **Production** track as a DRAFT release.
6. Open Play Console → Production → review the draft → "Start rollout".

For a re-run (e.g. CI failed mid-upload), use `workflow_dispatch` from the
Actions tab and pass the existing tag name.

## Required GitHub Secrets

Configure these under repo Settings → Secrets and variables → Actions:

| Secret | What it is | How to encode |
|---|---|---|
| `ANDROID_KEYSTORE_BASE64` | The `.jks` keystore | `base64 -i path/to/keystore.jks \| pbcopy` |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore store password | plain text |
| `ANDROID_KEY_ALIAS` | Alias inside the keystore | plain text |
| `ANDROID_KEY_PASSWORD` | Key password | plain text |
| `PLAY_SERVICE_ACCOUNT_JSON_BASE64` | Play Console service account JSON | `base64 -i path/to/play.json \| pbcopy` |
| `FEEDBACK_RECIPIENTS` | Comma-separated emails for the in-app feedback form | plain text |

The workflow base64-decodes the keystore + JSON into `$RUNNER_TEMP/secrets/`,
which GitHub wipes after the job ends.

## Why tag-driven instead of bumping in feature branches

- **No conflicts** between parallel feature branches on the versionCode line.
- **Cannot forget to bump** — if there's no tag, there's no release.
- **CI never writes back to the repo** — branch protections stay unbroken.
- **Reproducible**: rebuilding `v0.3.0` is `git checkout v0.3.0 && bundle exec
  fastlane android release_from_tag` (with `GITHUB_REF_NAME=v0.3.0` exported).
- The release-trigger workflow is decoupled from the merge workflow.
