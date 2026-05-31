# Fastlane iOS setup for Kropia

Run iOS lanes from `iosApp/`:

```bash
cd iosApp
bundle exec fastlane ios bootstrap_match
bundle exec fastlane ios match_certificates
bundle exec fastlane ios build
bundle exec fastlane ios beta
bundle exec fastlane ios upload_store_assets
```

If your shell picks the system Ruby inside `iosApp/`, use:

```bash
rbenv exec bundle exec fastlane lanes
```

## Shared match repository

Use the same private match repository as the other AppToLast apps on the same
Apple Developer Team. match separates provisioning profiles by bundle id, so
Kropia uses `com.apptolast.greenhousefronts` inside the shared repo.

Run `bootstrap_match` once to create/update the App Store profile. After that,
local build/beta lanes and CI use match in readonly mode.

## Local secrets

```bash
cp iosApp/fastlane/.env.example iosApp/fastlane/.env
```

Fill in:

- `APPLE_ID`
- `FASTLANE_TEAM_ID`
- `FASTLANE_ITC_TEAM_ID`
- `APP_STORE_CONNECT_API_KEY_ID`
- `APP_STORE_CONNECT_API_KEY_ISSUER_ID`
- `ASC_API_KEY_PATH`
- `MATCH_GIT_URL`
- `MATCH_PASSWORD`

## Metadata and screenshots

Store metadata under:

```text
iosApp/fastlane/metadata/en-GB/
iosApp/fastlane/metadata/es-ES/
```

Store App Store screenshots under:

```text
iosApp/fastlane/screenshots/en-GB/
iosApp/fastlane/screenshots/es-ES/
```

PNG/JPG screenshots are gitignored by default. Keep only screenshots you want
App Store Connect to receive, then run:

```bash
cd iosApp
bundle exec fastlane ios upload_store_assets
```
