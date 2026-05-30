# App Store screenshots

Place manually captured screenshots in one folder per locale:

```text
fastlane/screenshots/en-GB/
fastlane/screenshots/es-ES/
```

Suggested filenames:

```text
iPhone_01_login.png
iPhone_02_home.png
iPhone_03_greenhouse_detail.png
iPhone_04_alerts.png
iPad_01_dashboard.png
```

Run `bundle exec fastlane ios upload_store_assets` from `iosApp/` to upload
the screenshots that are present. PNG/JPG screenshots are gitignored by
default, so add only the images you want to send to App Store Connect.
