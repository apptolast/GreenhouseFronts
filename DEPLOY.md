# Deployment guide — GreenhouseFronts

Protocol for shipping the Android app to Google Play. iOS will be added when
the Apple Developer account is ready.

> **TL;DR** — branch your work as usual, merge through `develop` → `main`
> with squash-merges, then tag `vMAJOR.MINOR.PATCH` on `main`. The tag push
> is the only release trigger. Fastlane reads the tag, computes the next
> `versionCode` from Play, builds a signed AAB and uploads it to the
> Production track as DRAFT. You click "Start rollout" in Play Console.

---

## 1. Why this protocol exists (the migration story)

The previous flow asked the developer to:

- Manually bump `versionCode`/`versionName` in `composeApp/build.gradle.kts`
  inside every feature branch.
- Create a `deploy/<version>` branch off `develop`.
- Run `git rebase -i` and drop commits that shouldn't ship.
- Open a PR from `deploy/...` to `main`.
- Rely on a CI workflow triggered by the merge to `main`.

This produced a beautifully linear history but had real costs:

- **Conflicts on the `versionCode` line** when two feature branches were in
  flight at once. The second to merge had to rebase.
- **Forgotten bumps**, only caught after Play rejected the AAB.
- **Manual rebase-drop** was a ritual whose root cause was that `develop`
  history was being copied wholesale into the deploy branch — feature flags
  or proper `release/*` branches with cherry-picks would have removed the
  need entirely.
- The pipeline was tightly coupled to a `develop` → `deploy/...` → `main`
  branch shape that does not map cleanly to GitHub Actions or to multiple
  parallel feature streams.

The new protocol decouples **versioning** from **branching**:

- Code in the repo never carries release-version state. `build.gradle.kts`
  defaults exist only for local debug builds.
- The Git tag is the single source of truth for `versionName`.
- `versionCode` is computed at release time by querying Play (`max +1`).
- CI never writes back to the repo. Branch protection stays unbroken.
- Releases are reproducible: `git checkout v0.3.0 && fastlane …` builds the
  exact AAB Play received.

---

## 2. Versioning model

| | `versionName` | `versionCode` |
|---|---|---|
| Source of truth | The Git tag (`v1.2.3` → `1.2.3`) | `max(existing on all Play tracks) + 1` |
| Visible to users | Yes (Play Store listing) | No (Google-internal monotonic counter) |
| Changes per release | You decide (semver) | Always `+1` |
| Stored in `build.gradle.kts` | Default for debug only | Default for debug only |

**Major / minor / patch jumps are decided exclusively by the tag you create.**
There is no separate "bump major" command; you simply tag with the version
you want:

```bash
git tag v0.3.0   # minor — adding features
git tag v0.2.1   # patch — bugfix only
git tag v1.0.0   # major — breaking change or product readiness milestone
```

`versionCode` is independent of the semver jump — it just goes `+1` every
release. Tagging `v0.2.5` after `v0.2.4` and tagging `v2.0.0` after `v0.2.4`
both produce `versionCode = previous + 1`.

---

## 3. Branch model (unchanged)

GitFlow with linear history via squash-merges:

```
main      stable, what's on Play. PR-only. Tagged on each release.
develop   stable dev integration. PR-only. Squash-merge from feature/*.
feature/* one branch per task / Jira ticket.
hotfix/*  branched from main when production needs an urgent fix; merged
          back to both main and develop.
```

Branch protections (configure on GitHub):

- `main`: require PR, require approval, require linear history, restrict who
  can push tags (admin only is fine).
- `develop`: require PR, require approval, require linear history.
- Force-push disabled on both.

---

## 4. One-time setup

### 4.1. Local machine

```bash
# Ruby toolchain (managed via rbenv, version pinned in .ruby-version)
brew install rbenv ruby-build
echo 'eval "$(rbenv init - zsh)"' >> ~/.zshrc
source ~/.zshrc
rbenv install $(cat .ruby-version)

# Bundler + gems (Gemfile + Gemfile.lock at repo root)
gem install bundler -v "~> 2.5"
bundle install

# Local fastlane secrets
cp composeApp/fastlane/.env.example composeApp/fastlane/.env
# Edit .env so SUPPLY_JSON_KEY points at your local Play Console JSON key.
```

### 4.2. GitHub Secrets

Repo → Settings → Secrets and variables → Actions → New repository secret:

| Secret | Source | Format |
|---|---|---|
| `ANDROID_KEYSTORE_BASE64` | The release `.jks` keystore | `base64 -i path/to/keystore.jks \| pbcopy` |
| `ANDROID_KEYSTORE_PASSWORD` | `local.properties:signing.storePassword` | plain text |
| `ANDROID_KEY_ALIAS` | `local.properties:signing.keyAlias` | plain text |
| `ANDROID_KEY_PASSWORD` | `local.properties:signing.keyPassword` | plain text |
| `PLAY_SERVICE_ACCOUNT_JSON_BASE64` | Service account JSON downloaded from Google Cloud | `base64 -i path/to/play.json \| pbcopy` |
| `GOOGLE_SERVICES_JSON_BASE64` | Firebase config (`composeApp/google-services.json`) | `base64 -i composeApp/google-services.json \| pbcopy` |

The workflow base64-decodes the keystore + JSON into `$RUNNER_TEMP/secrets/`,
which GitHub wipes after each job.

### 4.3. Play Console (one-time)

- The Google Play Android Developer API is enabled in the GCP project that
  owns the service account (already done — needed once per project).
- The service account is granted "Release manager" (or stricter) access in
  Play Console → Setup → API access. Already done.
- The app already has at least one manually-uploaded AAB on each track you
  want fastlane to publish to. (Play does not allow `supply` to create the
  initial listing.)

---

## 5. Daily release flow

### 5.1. Develop a feature

```bash
git checkout develop && git pull
git checkout -b feature/MY-TICKET-short-description
# … work, commit …
git push -u origin feature/MY-TICKET-short-description
gh pr create --base develop          # squash-merge after review
```

You **never** touch `versionCode`/`versionName` manually any more.

### 5.2. Push a build to internal QA mid-development (optional)

When the feature is in a state your QA team can install on a real device:

```bash
cd composeApp
bundle exec fastlane android internal
```

> **Important**: always run fastlane from `composeApp/`. Bundler walks up to
> find the root `Gemfile`, but fastlane does NOT walk up to find the
> `fastlane/` folder — it looks only in cwd. The CI workflow handles this
> via `working-directory: composeApp` on the fastlane step.

This builds the current branch with `versionCode = next_from_play` and a
clearly-marked `versionName` (`<default>-internal-<code>`), then uploads it
to the Internal testing track as DRAFT. You can also pass a custom label:

```bash
bundle exec fastlane android internal version_name:"0.3.0-rc1"
```

The next CI release will compute `next + 1` from whatever was uploaded last,
so internal builds and production releases never collide on `versionCode`.

### 5.3. Cut a release

When `develop` is ready to ship:

```bash
# Merge develop → main via PR (squash).
gh pr create --base main --head develop --title "Release: ship develop to main"
# … review, approve, merge …

# Tag the release commit on main.
git checkout main && git pull
git tag v0.3.0 -m "Release 0.3.0: alert notifications, in-app feedback"
git push --tags
```

Pushing the tag triggers `.github/workflows/android-release.yml`, which:

1. Checks out the tagged commit.
2. Sets up JDK 17 + Ruby (cached).
3. Decodes secrets into the runner's temp dir.
4. Runs `bundle exec fastlane android release_from_tag`:
   - Reads the tag (`v0.3.0`) → `versionName = "0.3.0"`.
   - Queries Play tracks → `versionCode = max + 1`.
   - Calls Gradle with `-PappVersionCode=N -PappVersionName=X`.
   - Uploads the signed AAB to the **Production** track as DRAFT.
5. Uploads the AAB as a workflow artifact (30-day retention).

### 5.4. Roll out

Open Play Console → Production → review the draft → "Start rollout" (full or
staged %).

---

## 6. Common scenarios

### Hotfix on production

```bash
git checkout v0.3.0
git checkout -b hotfix/v0.3.1-fix-login
# … fix, commit …
gh pr create --base main             # PR straight to main, squash-merge

git checkout main && git pull
git tag v0.3.1 -m "Hotfix 0.3.1: login crash"
git push --tags
# CI publishes; remember to also merge the fix back into develop:
git checkout develop && git pull
git merge --ff-only main             # or open a back-merge PR
git push
```

### Major version bump

Identical to a normal release — you just pick a different tag:

```bash
git tag v1.0.0 -m "Release 1.0.0: GA"
git push --tags
```

`versionCode` is still `previous + 1`; semantic meaning lives only in the
tag you chose.

### Re-running a failed release

If CI fails after the tag was pushed (e.g. transient Play upload error), use
**Actions → Android Release → Run workflow** with the existing tag name as
the input. The workflow re-checks out the same tag and re-runs fastlane.
Idempotent because:

- `versionCode` is recomputed from Play, so it skips ahead if a partial
  upload already consumed a code.
- The build is deterministic from the tagged commit.

### Re-building locally for forensics

```bash
git checkout v0.3.0
GITHUB_REF_NAME=v0.3.0 SUPPLY_JSON_KEY=/path/to/play.json \
  bundle exec fastlane android release_from_tag
```

Produces the same AAB the runner uploaded. Useful for debugging crash reports
that pin a specific `versionCode`.

---

## 7. File map

| Path | Role |
|---|---|
| `Gemfile` / `Gemfile.lock` | Pinned Ruby gems (fastlane). |
| `.ruby-version` | Pinned Ruby interpreter version (read by rbenv + CI). |
| `composeApp/fastlane/Fastfile` | Lanes: `version`, `validate_play`, `build`, `internal`, `release_from_tag`. |
| `composeApp/fastlane/Appfile` | Static identifiers: `package_name`, `json_key_file`. |
| `composeApp/fastlane/.env.example` | Documents required local env vars. Tracked. |
| `composeApp/fastlane/.env` | Local env vars (gitignored). |
| `composeApp/fastlane/SETUP.md` | Local-setup-focused doc (subset of this file). |
| `composeApp/build.gradle.kts` | Reads `appVersionCode` / `appVersionName` properties; defaults are debug-only. |
| `.github/workflows/android-release.yml` | Tag-triggered release workflow. |

---

## 8. Migrating from this branch to the new protocol — first release

This is the procedure for the **very first** release under the new protocol
(starting from `feature/alert-notifications`):

1. Open a PR `feature/alert-notifications` → `develop` and squash-merge.
2. Open a PR `develop` → `main` and squash-merge.
3. On `main`:
   ```bash
   git checkout main && git pull
   git tag v0.3.0 -m "Release 0.3.0: alert notifications, in-app feedback, critical heartbeat"
   git push --tags
   ```
4. Watch the Actions tab — the workflow should succeed in ~5–8 minutes.
5. Open Play Console → Production → review the draft → "Start rollout".

After this first release, every subsequent release follows §5 verbatim.
