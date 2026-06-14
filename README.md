# Kaspotify

A Spotify-style player for music that's **already on your device** — an offline
importer, not a streaming client. No accounts, no network calls, no streaming.

Built with Kotlin, Jetpack Compose (Material 3, dark), Media3/ExoPlayer for
background playback, Room for local state, and Hilt for dependency injection.

## Features

- **Offline importer** — scans device audio via `MediaStore` (music only, skips
  ringtones and clips under 5s) and derives albums & artists.
- **Background playback** — a Media3 `MediaSessionService` with lock-screen /
  notification controls, audio focus, and auto-pause when headphones unplug.
- **Library** — tabs for Songs / Albums / Artists / Favorites, plus *shuffle all*.
- **Search** across title, artist, and album.
- **Playlists** — create, open, add a song from its ⋮ menu, remove.
- **Favorites, play counts, recently played** — persisted in Room.
- **Now Playing** — large artwork, scrubbable seek bar, shuffle, repeat
  (off/all/one), favorite toggle, and a sleep timer (15/30/45/60 min).
- **Mini-player** docked above the bottom navigation with a live progress bar.
- **Queue** — play next / add to queue.

## Architecture

```
data/
  model/        Plain data classes: Song, Album, Artist, Playlist
  local/        Room entities, DAO, database (favorites, play counts, recents, playlists)
  media/        MediaStoreImporter — reads device audio
  repository/   MusicRepository — combines importer + Room into reactive Flows
playback/
  PlaybackService   Media3 MediaSessionService (ExoPlayer + audio focus)
  PlayerController   @Singleton the UI talks to (current song, isPlaying, position, …)
ui/
  theme/        Dark Material 3 theme (Spotify-ish green)
  components/   Artwork, SongRow, MiniPlayer
  screens/      Library, Search, Playlists, PlaylistDetail, NowPlaying
  MusicViewModel, AppScaffold (bottom nav + overlays)
di/             Hilt modules
```

## Requirements

- Android Studio (Koala or newer) **or** a command-line setup with:
  - JDK 17
  - Android SDK with **platform `android-34`** and **build-tools `34.0.0`**
- minSdk 24, targetSdk/compileSdk 34.

## Build

```bash
./gradlew assembleDebug
# APK -> app/build/outputs/apk/debug/app-debug.apk
```

If `gradlew` reports "SDK location not found", create `local.properties`:

```properties
sdk.dir=/path/to/Android/Sdk
```

## Install & run

```bash
# install on a connected device / emulator
./gradlew installDebug
# or
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

On first launch the app shows a rationale screen and requests audio access
(`READ_MEDIA_AUDIO` on Android 13+, `READ_EXTERNAL_STORAGE` below). Grant it and
the library imports automatically.

## Load test audio

The app only shows music that exists on the device, so push a few files:

```bash
adb push your-track.mp3 /sdcard/Music/
# make MediaStore notice the new file(s)
adb shell cmd media_scanner scan /sdcard/Music     # Android 12+
# fallback on older devices:
adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE \
  -d file:///sdcard/Music/your-track.mp3
```

Then pull-to-refresh isn't needed — re-granting/relaunching re-imports, or the
library re-scans on next launch.

## Permissions

| Permission | When |
|---|---|
| `READ_MEDIA_AUDIO` | Android 13+ (API 33+) |
| `READ_EXTERNAL_STORAGE` (maxSdk 32) | Android 12 and below |
| `POST_NOTIFICATIONS` | Android 13+, for the playback notification |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | background playback |
