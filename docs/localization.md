# Localization

The application uses Android resources for the Android shell, notifications and
widgets, and Compose Multiplatform resources for shared screens. Turkish uses
the `tr` tag and `values-tr/` directories. The ISO-639/2 media-language preference
`tur` is separate from the interface-language tag.

## Turkish resources

The default resource files have matching Turkish files in all 38 resource
locations: 5,247 strings and 26 plural resources at the time of this change.
Non-translatable branding, identifiers and technical constants continue to use
their default resources.

An automated first draft was revised for authentication, onboarding, home,
subtitle selection/styling, seeking, casting and player terminology. The player
chapter/decoder/playback-speed/rendering sheets now resolve their residual
English literals through resources. Player option labels are localized without
changing serialized enum names or engine protocol keys. Detailed wording on
less frequently used administration screens can still benefit from further
contextual review.

Phone/TV navigation and the desktop rail resolve their labels through
`Route.localizedNavLabel()` at composition time; registry labels and persisted
customization keys remain stable. Built-in home feed headers also resolve when
rendered, retaining custom discovery, pinned and server plugin row titles.
The subtitle style preview uses localized sample text, including Turkish glyphs.

| English concept | Turkish UI wording |
| --- | --- |
| Library | Kitaplık |
| TV show / series | Dizi |
| Season / episode | Sezon / bölüm |
| Player / client | Oynatıcı / istemci |
| Seek forward / back | İleri / geri sar |
| Cast to another device | Başka cihazda oynat / yayınla |
| Subtitle delay | Altyazı gecikmesi / zaman farkı |
| Download lookahead | Önden indirilecek bölüm sayısı |
| System default | Sistem varsayılanı |

Turkish is registered in both `appLanguages` in `LanguageSettingsScreen.kt` and
the Android `localeFilters`. The null/default picker entry resolves the existing
localized system-default resource. Desktop still follows the OS locale; this
change does not add a desktop app-language override.

## Updating resources

Add a default resource and its Turkish counterpart in the same module and file.
Use UTF-8, retain resource names/attributes and argument types (`%1$s`, `%1$d`,
etc.), and preserve newline escapes and plural quantities. Escape quotes in
native Android resource text as required by aapt. Do not translate configuration
keys such as `sub-ass-override`, URLs, product names or shader algorithm names.

```powershell
python tools/localization/validate_resources.py tr
.\gradlew.bat :app:verifyPhoneDebugComposeResources
.\gradlew.bat :shared:feature:settings:jvmTest --tests '*LanguageSettingsViewModelTest'
.\gradlew.bat :shared:core:ui:testAndroidHostTest --tests '*LocaleApplier*' --init-script tools/dev/host-test-locale.init.gradle
```

The resource validator checks exact file/key coverage, duplicate names, resource
types and attributes, argument multisets, newline escapes, plural quantities and
unresolved draft markers. The APK guard checks that localized Compose resource
assets actually ship in the phone APK. Neither check establishes visual layout
or playback behavior.

## Device verification

Use the installed debug application `com.raulshma.jellyplay.dev`. Keep application
data and any existing Jellyfin login when installing the updated APK with
`adb install -r`. On Android 13+, the shell's per-app locale commands can exercise
the system language-change path without changing the phone's system language:

```powershell
adb -s <serial> shell cmd locale set-app-locales com.raulshma.jellyplay.dev --locales tr
adb -s <serial> shell cmd locale get-app-locales com.raulshma.jellyplay.dev
```

Verify Turkish and English screens, returning to Turkish, and Turkish after a
cold launch. Check Turkish glyphs, long labels and text fields. Once signed into
Jellyfin, also verify the in-app interface-language picker, subtitle/audio
settings, media details and player sheets. Keep authentication and actual
playback results separate from language rendering.

On 2026-10-17, the Redmi Note 11 Pro (Android 13/API 33, ARM64, system locale
`tr-TR`) reached the PC's existing Jellyfin server over the LAN. Quick Connect
authenticated the existing account. The server form, login, onboarding, home,
media details and language/subtitle settings rendered Turkish. The in-app
language picker switched to English and back to Turkish while preserving the
session. After the final APK update (`adb install -r`) and a force-stop/cold
launch, the session and explicit `tr` app locale were retained. Built-in home
headers and navigation labels rendered Turkish; the subtitle preview displayed
Turkish glyphs and the shortened toggle labels fitted the device width. Local
screenshots and UI dumps are in the ignored `build/device-turkish/` directory.

The language view-model and locale-applier suites passed 10 tests. The existing
navigation registry, visible-routes and customization suites passed 26 tests.
The complete XML/format validator and phone APK resource guard also passed.

Media titles, descriptions, genre names and user-defined library names come
from Jellyfin and retain their server-provided language. UI translation does
not rewrite server metadata. Some inherited hardcoded accessibility descriptions,
administration labels and enum summaries still need separate migration. Actual
video playback, subtitle synchronization, downloads, Android TV and desktop
runtime behavior were not established by this localization smoke test.
