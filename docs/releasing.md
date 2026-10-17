# Android phone releases

The first JellyPlay+ release is built locally and uploaded to GitHub Releases.
The inherited `release.yml` workflow also builds TV and desktop packages and
requires upstream-style repository signing secrets; it is not used for this cut.

## Signing

The release keystore is `jellyplay-release.jks` at the repository root and is
ignored by Git. A local backup is stored at
`%USERPROFILE%\.android\jellyplay-plus-release.jks`.
The alias is `jellyplay-plus`. The password is stored as a Windows DPAPI-protected
PowerShell credential at
`%USERPROFILE%\.android\jellyplay-plus-release-signing.clixml`.
Only the same Windows user can decrypt that credential on the current machine.
Keep a secure, recoverable backup of the keystore and password before moving
machines; future updates must use the same signing key. Never commit or upload
these signing files as release assets.

## Build

Choose a new version name and a strictly increasing version code. Version 0.1.0
uses code 100. From PowerShell at the repository root:

```powershell
$credential = Import-Clixml "$env:USERPROFILE\.android\jellyplay-plus-release-signing.clixml"
$env:KEYSTORE_PASSWORD = $credential.GetNetworkCredential().Password
$env:KEY_PASSWORD = $env:KEYSTORE_PASSWORD
$env:KEY_ALIAS = $credential.UserName
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
try {
    .\gradlew.bat :app:assemblePhoneRelease '-PversionName=0.1.0' '-PversionCode=100'
    if ($LASTEXITCODE -ne 0) { throw 'Release build failed.' }
} finally {
    Remove-Item Env:KEYSTORE_PASSWORD, Env:KEY_PASSWORD, Env:KEY_ALIAS -ErrorAction SilentlyContinue
}
```

Verify the resulting APK with the Android SDK's `apksigner verify --verbose
--print-certs` and `aapt dump badging`. Confirm the package, version, ABI, and
signing certificate before uploading it. Keep R8 mappings locally for crash
diagnosis. Create SHA-256 checksums for the public APK assets.

## Publish

Commit the reviewed sources, tests, database schema, and release notes. Push the
commit and an annotated version tag. Create a GitHub release for that exact tag
and upload the APK and checksum file. Do not upload local diagnostic databases,
device fixtures, signing files, or build logs containing user data. Verify the
published tag, version, asset sizes, and checksums after publication.

The release application ID is `com.raulshma.jellyplay`, separate from the local
development app's `com.raulshma.jellyplay.dev`. This fork's release key also differs
from upstream JellyPlay's key; upstream installations cannot be updated in place.
