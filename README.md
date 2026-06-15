# Kaspotify

[![Build APK](https://github.com/KasraJamei/Kaspotify/actions/workflows/build.yml/badge.svg)](https://github.com/KasraJamei/Kaspotify/actions/workflows/build.yml)

A Spotify-style player for music that's **already on your device** — an offline
importer, not a streaming client. No accounts, no network calls, no streaming.

Built with Kotlin, Jetpack Compose (Material 3, dark), Media3/ExoPlayer for
background playback, Room for local state, and Hilt for dependency injection.

## Design

Kaspotify uses a **minimal monochrome** look: deep-black surfaces and a
platinum-white accent, with a soft, album-art–driven gradient backdrop behind
the content (no expensive blur). Controls stay white-on-black; only the ambient
glow shifts with the music. The UI is built for smoothness — it requests the
display's highest refresh-rate mode and keeps recomposition tightly scoped so
lists and animations stay fluid.

## Features

- **Offline importer** — scans device audio via `MediaStore` (music only, skips
  ringtones and clips under 5s) and derives albums & artists, including bitrate/
  format metadata for quality badges (Lossless, 320 kbps, AAC, …).
- **Background playback** — a Media3 `MediaSessionService` with lock-screen /
  notification controls, audio focus, and auto-pause when headphones unplug.
- **Library** — tabs for Songs / Albums / Artists / Favorites, plus *shuffle all*
  and **smart playlists** (Playlist of the Day, Recently Added, Most Played).
- **Most Played by week** — play counts roll on a 7-day window, so Most Played
  reflects what you're listening to *now*; each row notes "Listened N times this
  week".
- **Search** across title, artist, and album, with saved recent searches.
- **Playlists** — create, open, add a song from its ⋮ menu, remove, plus a
  builder that assembles a playlist by source and quality.
- **By Quality** browser — your library grouped into audio-quality tiers.
- **Favorites, play counts, recently played** — persisted in Room.
- **Now Playing** — large artwork with a spring entrance, an album-art gradient
  hero, scrubbable seek bar, shuffle, repeat (off/all/one), favorite toggle
  (incl. double-tap-to-favorite with a heart pop), and a sleep timer.
- **Audio visualizer** — a smooth FFT spectrum with log-spaced bands and natural
  attack/gravity-decay motion (requires `RECORD_AUDIO`, requested on demand).
- **Equalizer** — 5-band EQ with presets.
- **Effects** — Slow + Reverb mode (adjustable 0.5x–1.25x playback speed plus
  `PresetReverb` rooms/halls/plate), including a one-tap "Slowed + Reverb" toggle.
- **Queue screen** — view the up-next queue, drag to reorder, swipe to remove.
- **Gestures** — swipe a song row right to add to queue, left to play next.
- **In-app notifications** — a minimal glass snackbar confirms actions like
  add-to-queue, play-next, like/unlike, and playlist changes.
- **Dynamic ambient** — the gradient backdrop animates to match the current
  album art, while the accent stays a constant platinum.
- **Floating mini-player + glass navigation** — docked above a responsive,
  inset-aware floating nav bar, with animated artwork and a live progress bar.
- Minimal platinum-white "K" monogram adaptive app icon.

## Architecture

```
data/
  model/        Plain data classes: Song, Album, Artist, Playlist
  local/        Room entities, DAO, database (favorites, play/weekly counts, recents, playlists, search)
  media/        MediaStoreImporter — reads device audio (incl. mime/bitrate/path)
  repository/   MusicRepository — combines importer + Room into reactive Flows
playback/
  PlaybackService       Media3 MediaSessionService (ExoPlayer + audio focus)
  PlayerController       @Singleton the UI talks to (current song, isPlaying, position, queue, …)
  EqualizerController     5-band EQ bound to the ExoPlayer audio session
  VisualizerController    FFT capture reduced to log-spaced frequency-band levels
  ReverbController         PresetReverb for the Slow + Reverb effects mode
ui/
  theme/        Minimal-mono Material 3 theme + Palette-based ambient color (LocalAmbientColor)
  components/   Artwork, SongRow, MiniPlayer, QualityBadge, VisualizerView, Surfaces (GradientBackdrop/GlassCard/…)
  screens/      Library, Search, Playlists, PlaylistDetail, SmartPlaylist, Quality,
                 AlbumDetail, ArtistDetail, NowPlaying, Equalizer, Effects, Queue
  MusicViewModel, AppScaffold (floating nav + mini-player + overlays + snackbars)
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

> The APKs attached to GitHub Releases are signed with a **stable bundled update
> key**, so each release installs over the previous one without uninstalling. It
> is not a Play-Store release key (its password is public); to publish, configure
> the `KEYSTORE_*` secrets and they take precedence automatically.
>
> Note: the very first build that switched to this key (v1.5.4) requires a
> one-time uninstall of any older Kaspotify; updates after it install cleanly.

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
| `RECORD_AUDIO` | requested on demand when enabling the audio visualizer |
| `MODIFY_AUDIO_SETTINGS` | for the equalizer and slow+reverb effects |

## Development & authorship

This project was developed end-to-end by **Claude** (Anthropic's Claude Code).
Every part of it — the application code, the architecture, the visual design, the
app icon, the CI configuration, and this README — was written by the AI. The
human author's role was direction: describing goals and preferences in
natural-language prompts, reviewing the results, and requesting changes. In other
words, the prompts are human; the implementation is Claude's.
