# JellyPlay+ development

JellyPlay+ is the Salih800/jellyplay-plus fork of
[JellyPlay](https://github.com/raulshma/jellyplay). Visible client branding is
JellyPlay+ (JellyPlay+ Dev for Android debug builds). Internal Kotlin packages,
Android application IDs, persisted data paths, and companion-plugin names stay
compatible with upstream.

## Local tools

The following tools were already available on the development PC on 2026-10-17:

| Tool | Purpose |
| --- | --- |
| JDK 21 | Gradle daemon, selected by gradle/gradle-daemon-jvm.properties |
| JDK 17 in the Gradle toolchain cache | JVM compilation and tests |
| Gradle 9.7.1 wrapper | Project configuration, compilation, tests and APK packaging |
| Android SDK platform/build tools 37 | Compile the Android application |
| Android platform-tools / adb | Install, launch, inspect logs, capture screens and drive device tests |
| Git | Inspect and maintain the fork |
| Python 3 | Optional resource/XML validation scripts |
| 7-Zip | Extract the bundled Windows libmpv archive for desktop work |

Android Studio is optional for command-line development. No new library is
required for branding, the fork update feed, or Turkish resources. Gradle may
need to fetch existing declared dependencies that are absent from the local
cache. An emulator is optional for additional Android/API levels; TV navigation
and hardware-specific playback need separate test targets.

For PowerShell sessions where SDK discovery is not configured:

```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
```

This is a process-local setting; it does not change the system environment.

## Android test device

The connected Xiaomi Redmi Note 11 Pro (2201116TG) reports Android 13 / API 33,
ARM64, approximately 8 GB RAM, a 1080 x 2400 screen, and Turkish system locale.
It is suitable for phone UI, locale switching, subtitles, downloads, and
real playback tests. Available storage was approximately 89 GB at inspection.

On this MIUI device, normal USB debugging permits ADB inspection but input
injection is separately gated by **USB debugging (Security settings)**.
Without that setting, `adb shell input` fails with `INJECT_EVENTS`; enable it
on the phone before automated taps or key events. **Install via USB** and an
on-device confirmation may also be required for APK installation. These are
device developer settings, not additional PC software.

The device does not establish Android TV behavior, pre-Android-13 locale
handling, or playback support on other chipsets. Successful installation and
launch do not establish authenticated Jellyfin playback.

```powershell
.\gradlew.bat :app:assemblePhoneDebug
adb devices -l
```

Use `adb -s <serial>` for every device action if more than one device is
connected. Install the generated ARM64 phone debug APK with `adb install -r`,
then verify its label and launch the application. Do not clear app data or
uninstall an existing build to work around a signing conflict without reviewing
the existing installation first.

## Fork updates

The shared update client reads releases and release notes from
`https://api.github.com/repos/Salih800/jellyplay-plus/`. The URL allow-list pins
both that owner/repository and repository ID `1423313233`, including the current
GitHub release-asset CDN path. The upstream repository is not an update source.
The desktop fallback and About-screen release link point to the fork too.

At inspection the fork had no latest published release (HTTP 404). The existing
client reports an HTTP failure for that response; there is no release APK to
download or install yet. This is distinct from a successful check reporting that
an installed version is current.

For a real update test, publish a newer release with a higher version code,
the same application ID, and the same signing key as the installed release.
Future Android assets use `jellyplay-plus-v<version>-android-<abi>.apk`; Android
TV assets use `jellyplay-plus-v<version>-android-tv-<abi>.apk`. Keep identical
copies with the legacy `jellyplay-v<version>-phone-<abi>.apk` / `-tv-<abi>.apk`
names so installed clients through 0.1.3 can still find updates. See
[release naming and compatibility](releasing.md#asset-names-and-update-compatibility).
Android debug builds have the `.dev`
application-ID suffix and a debug signing key; a normal release APK does not
update that installation.

The inherited Android release workflow requires repository secrets
`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD`.
Signing keys must be retained for future updates. Do not commit keys or secret
values. No release, commit, tag, or push is needed to build and test locally.

## Focused verification

```powershell
.\gradlew.bat :shared:core:network:jvmTest --tests '*GitHubRepoAllowListTest' --tests '*GitHubReleasesApiImplTest'
.\gradlew.bat :shared:core:data:jvmTest --tests '*AppUpdateRepositoryImplTest'
.\gradlew.bat :app:testPhoneDebugUnitTest --tests '*UpdateCoordinatorTest'
.\gradlew.bat :apps:desktop:test --tests '*DesktopAppUpdateTest' --tests '*DesktopUpdateCheckControllerTest'
```

On Turkish Windows, Conscrypt can search for `conscrypt_openjdk_jni-wındows-x86_64`
instead of the bundled `windows` library before a Robolectric test starts.
Use the optional init script to set the locale of forked test JVMs only:

```powershell
.\gradlew.bat :app:testPhoneDebugUnitTest --tests '*UpdateCoordinatorTest' --init-script tools/dev/host-test-locale.init.gradle
```

This workaround does not change Windows language settings or application locale
behavior. Tests that set a locale explicitly can still exercise Turkish.

On 2026-10-17, the focused verification passed 49 tests: 14 release-feed tests,
14 URL allow-list tests, 11 update-download repository tests, and 10 Android
update-coordinator tests (using the host-test locale workaround). The ARM64
phone debug APK built successfully and its packaged label was verified as
`JellyPlay+ Dev`. These checks do not establish a real release-to-release
installation or authenticated Jellyfin playback.

The APK was installed on the Redmi Note 11 Pro and launched successfully.
The server-list screen rendered the `JellyPlay+` heading. After enabling the
MIUI input-security option, an ADB tap opened Add Server, the expected server
address/discovery form appeared, and Back returned to the original server list.
That initial smoke test stopped at the server list. The later Turkish-language
test connected to the existing PC Jellyfin server using Quick Connect; see
[localization](localization.md) for the current results. Local smoke-test
evidence is in the ignored `build/device-smoke/` and `build/device-turkish/`
directories.

After adding Turkish, verify XML keys, format arguments and plurals, run
`:app:verifyPhoneDebugComposeResources`, and test locale selection, cold launch,
layout overflow, and Turkish glyphs on the phone. Keep those results separate
from playback/subtitle/download validation against a Jellyfin server.
