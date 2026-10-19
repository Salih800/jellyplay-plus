# Android releases

JellyPlay+ Android phone releases are built locally and uploaded to GitHub Releases.
The inherited `release.yml` workflow also builds TV and desktop packages and
requires upstream-style repository signing secrets; it is not used for these cuts.

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

Choose a new version name and a strictly increasing version code. Published
version 0.1.5 uses code 105. The next-version example below uses 0.1.6/code 106;
check the latest release before choosing values. From PowerShell at the repository root:

```powershell
$releaseVersion = '0.1.6'
$releaseVersionCode = 106
$credential = Import-Clixml "$env:USERPROFILE\.android\jellyplay-plus-release-signing.clixml"
$env:KEYSTORE_PASSWORD = $credential.GetNetworkCredential().Password
$env:KEY_PASSWORD = $env:KEYSTORE_PASSWORD
$env:KEY_ALIAS = $credential.UserName
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
try {
    .\gradlew.bat :app:assemblePhoneRelease "-PversionName=$releaseVersion" "-PversionCode=$releaseVersionCode"
    if ($LASTEXITCODE -ne 0) { throw 'Release build failed.' }
} finally {
    Remove-Item Env:KEYSTORE_PASSWORD, Env:KEY_PASSWORD, Env:KEY_ALIAS -ErrorAction SilentlyContinue
}
```

Verify the resulting APK with the Android SDK's `apksigner verify --verbose
--print-certs` and `aapt dump badging`. Confirm the package, version, ABI, and
signing certificate before uploading it. The phone release package must be
`com.salih800.jellyplay.plus`; do not publish an APK with upstream's package ID.
Confirm launcher shortcuts and provider authorities resolve to the final package.
Keep R8 mappings locally for crash
diagnosis. Create SHA-256 checksums for the public APK assets.

## Asset names and update compatibility

Starting with 0.1.4, use these public asset names:

- Phones and tablets: `jellyplay-plus-v<version>-android-<abi>.apk`.
- Android TV / Fire TV: `jellyplay-plus-v<version>-android-tv-<abi>.apk`.
- Release title: `JellyPlay+ <version>`; Git tag: `v<version>`.

The Gradle flavors remain `phone` and `tv`; these are build identifiers, not the
public platform names. The updated client prefers the branded filenames and
still accepts older `jellyplay-v<version>-phone-<abi>.apk` / `-tv-<abi>.apk` assets.
ABI preference takes precedence over filename branding.

Installed clients through 0.1.3 only recognize the older exact names. Include
an identical compatibility copy under the old name in each future release so
those clients can discover the update. Do not remove compatibility copies until
support for those clients is deliberately retired. List both names in
`SHA256SUMS.txt`; do not rebuild or re-sign the copy. Published releases through
0.1.3 keep their existing filenames and tags.

For a local ARM64 Android cut, after verifying the APK from the build above:

```powershell
$releaseDirectory = "build/releases/v$releaseVersion"
New-Item -ItemType Directory -Path $releaseDirectory -Force | Out-Null
$primaryName = "jellyplay-plus-v$releaseVersion-android-arm64-v8a.apk"
$legacyName = "jellyplay-v$releaseVersion-phone-arm64-v8a.apk"
$sourceApk = 'app/build/outputs/apk/phone/release/app-phone-arm64-v8a-release.apk'
Copy-Item -LiteralPath $sourceApk -Destination "$releaseDirectory/$primaryName"
Copy-Item -LiteralPath $sourceApk -Destination "$releaseDirectory/$legacyName"
$apkHash = (Get-FileHash -LiteralPath "$releaseDirectory/$primaryName" -Algorithm SHA256).Hash.ToLowerInvariant()
"$apkHash  $primaryName", "$apkHash  $legacyName" |
    Set-Content -Encoding ascii "$releaseDirectory/SHA256SUMS.txt"
```

Upload both APK names and the checksum file. The inherited Android release
workflow follows the same naming, compatibility-copy, and checksum rules.

The launcher shortcut template is `app/src/main/shortcuts.xml`.
`GenerateLauncherShortcutsTask` writes a resource XML for each variant using its
final application ID. Android shortcut intent `targetPackage` requires a literal;
neither a string resource reference nor a manifest placeholder works there.

## Publish

Commit the reviewed sources, tests, database schema, and release notes. Push the
commit and an annotated version tag. Create a GitHub release for that exact tag
and upload the APK and checksum file. Do not upload local diagnostic databases,
device fixtures, signing files, or build logs containing user data. Verify the
published tag, version, asset sizes, and checksums after publication.

Since 0.1.2, the phone release application ID is `com.salih800.jellyplay.plus`,
separate from upstream's `com.raulshma.jellyplay` and the existing local development
app's `com.raulshma.jellyplay.dev`. The TV release uses
`com.salih800.jellyplay.plus.tv`. Debug variants explicitly preserve their legacy
phone/TV application IDs to retain existing development data. Source namespaces
remain unchanged.

Version 0.1.0 incorrectly reused upstream's application ID; 0.1.1 used
`com.raulshma.jellyplay.plus`. Their installed data does not automatically transfer
to the final application ID in 0.1.2. Do not uninstall existing apps or erase their
data to install this fork. Continue using the same JellyPlay+ signing key for
future `com.salih800.jellyplay.plus` updates.
