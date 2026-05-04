# UMAK — Complete App Specification

**Target Device:** Samsung Galaxy S24 Ultra  
**Platform:** Android 13+ (API 33+)  
**Architecture:** MVVM + Clean Architecture  
**Language:** Kotlin + Jetpack Compose  

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Architecture](#2-architecture)
3. [Screens & UI](#3-screens--ui)
4. [Features — Detailed Specification](#4-features--detailed-specification)
5. [Audio Engine](#5-audio-engine)
6. [Equalizer & Audio Effects](#6-equalizer--audio-effects)
7. [Volume Control](#7-volume-control)
8. [File Explorer](#8-file-explorer)
9. [Music Library](#9-music-library)
10. [Database Schema](#10-database-schema)
11. [Permissions](#11-permissions)
12. [Dependencies](#12-dependencies)
13. [Development Roadmap](#13-development-roadmap)
14. [Samsung S24 Ultra Hardware Notes](#14-samsung-s24-ultra-hardware-notes)

---

## 1. Project Overview

A personal HiFi music player app for Samsung Galaxy S24 Ultra with:
- Playback of MP3, FLAC, WAV, M4A, OGG, OPUS, APE, DSD, ALAC, WMA, AIFF
- Hardware-level DSP equalizer using Android AudioFX API
- 50-level logarithmic volume control
- Full filesystem explorer with file management
- Music library organized by album, artist, track, and playlist
- Hi-res audio path: up to 32-bit / 384kHz
- AMOLED-optimized dark UI

---

## 2. Architecture

```
app/src/main/java/com/musicplayer/
├── di/                    ← Hilt dependency injection modules
├── data/
│   ├── local/
│   │   ├── db/            ← Room database (entities, DAOs, AppDatabase)
│   │   ├── mediastore/    ← MediaStore scanner
│   │   └── filemanager/   ← File system operations
│   └── repository/        ← MusicRepository, FileRepository
├── domain/
│   └── model/             ← Track, Album, Artist, Playlist, EqPreset
├── service/               ← MusicPlayerService (MediaSessionService)
├── ui/
│   ├── screens/           ← One sub-package per screen
│   ├── components/        ← Shared composables
│   ├── navigation/        ← AppNavigation, Screen sealed class
│   └── theme/             ← Color, Type, Theme
└── util/                  ← VolumeUtil, AudioFormatUtil, TimeUtil
```

**Data flow:**
- UI (Compose) → ViewModel → Repository → (Room DB / MediaStore / File system)
- Audio playback: UI → ViewModel → MusicPlayerService (via ServiceConnection) → ExoPlayer → AudioFX → Hardware DAC

---

## 3. Screens & UI

### Navigation Structure

```
Bottom Navigation Bar:
  Home | Library | [Now Playing FAB] | Files | Settings
```

### Screen 1: Home Screen (`HomeScreen.kt`)

**Purpose:** Quick access hub for recently played and recently added content.

**Layout:**
- Top app bar: "HiFi Player" title, Refresh (library scan) and Settings icons
- `LazyColumn` with two sections:

**Section A — Recently Added (horizontal scroll):**
- `LazyRow` of `AlbumCard` composables
- Album art thumbnail, album name, artist, year
- Tap → Album Detail Screen

**Section B — Recently Played:**
- Vertical list of `TrackListItem` composables
- Shows last 20 played tracks
- Tap → plays the track and navigates to Now Playing

**Empty state:** First-launch prompt to scan library with a prominent button

---

### Screen 2: Library Screen (`LibraryScreen.kt`)

**Purpose:** Browse the full music library.

**Layout:**
- Search bar (always visible below top app bar)
- 4-tab `ScrollableTabRow`: Tracks / Albums / Artists / Playlists

**Tracks Tab:**
- `LazyColumn` of `TrackListItem`
- Each item shows: track number, title, artist, format badge (FLAC/WAV/hi-res), duration, favorite icon, more menu
- More menu options: Add to playlist, Add to queue, View file info, Share

**Albums Tab:**
- `LazyVerticalGrid` (2 columns on phone, 3+ on landscape) of `AlbumCard`
- Each card: square album art, album name, artist, year
- Long-press: play all, shuffle, add to queue

**Artists Tab:**
- `LazyColumn` of artist list items
- Artist name, album count, track count
- Tap → Artist Detail Screen

**Playlists Tab:**
- `LazyColumn` of playlist items
- Playlist name, description, track count
- FAB to create new playlist
- Long-press → rename or delete

---

### Screen 3: Now Playing Screen (`NowPlayingScreen.kt`)

**Purpose:** Full playback control with all audio adjustments.

**Layout (top to bottom):**
1. Top bar: collapse arrow (←), EQ button, sleep timer button
2. **Album art** — full-width square with rounded corners, blurred background
3. **Track info row:** title (large), artist (medium), format badge (e.g. `FLAC 24-bit / 96 kHz`)
4. Favorite heart icon (right side of info row)
5. **Progress scrubber:** slider + current time / total time labels
6. **Transport controls row:** Shuffle | Previous | Play/Pause (large FilledIconButton) | Next | Repeat
7. **Volume slider** (`VolumeSlider` composable) — 50 levels, logarithmic, with dB readout
8. Queue / playlist access button

**Shuffle:** toggles ExoPlayer shuffle mode  
**Repeat:** cycles OFF → REPEAT_ONE → REPEAT_ALL  
**Format badge:** computed from `AudioFormatUtil.formatBadge()`, highlighted in primary color if hi-res

---

### Screen 4: Equalizer Screen (`EqualizerScreen.kt`)

**Purpose:** Fine-tune audio at hardware DSP level.

**Layout:**
- Top bar: back arrow, EQ on/off Switch, Save Preset (+) button
- **Preset chips row** — horizontal scrollable; tap to apply
- **Per-band sliders** — one per hardware band (typically 5 on S24 Ultra)
  - Each shows: frequency label (e.g. "60 Hz"), current gain (e.g. "+3.0 dB")
  - Slider range: minMillibel to maxMillibel (queried from hardware, ±1500 mB typical)
- **Bass Boost** slider: 0–1000 strength
- **Virtualizer** slider: 0–1000 strength (applies 3D effect, useful with headphones)
- **Loudness Enhancer** slider: 0–1000 mB gain

**Preset management:**
- Built-in presets: Flat, Bass Boost, Vocal Clarity, Rock, Classical, Electronic
- User can save current settings as named preset
- User presets can be deleted (built-in ones cannot)

---

### Screen 5: File Explorer Screen (`FileExplorerScreen.kt`)

**Purpose:** Full file system browser for music file organization.

**Layout:**
- Top bar: back arrow, multi-select action buttons (copy/cut/clear when items selected), paste when clipboard active
- **Breadcrumb bar** — horizontal scrollable path: `Storage / Music / Albums / Artist`
- **File list** — `LazyColumn` of `FileListItem`
- **Bottom toolbar** — Up, item count / selection count, New Folder button

**File List Item:**
- Icon: folder (blue) / audio file (primary color) / other file (gray)
- Name, size (for files)
- Long-press to enter selection mode
- More menu: Play (audio), Add to playlist, Rename, Cut, Copy, Delete, Properties

**Selection mode:**
- Checkbox or highlight on selected items
- Top bar switches to action bar (copy/cut/delete selected)

**Properties dialog:**
- File path, size, format, sample rate, bit depth, bitrate, duration
- ID3 tag info if available

---

### Screen 6: Album Detail Screen (`AlbumDetailScreen.kt`)

**Layout:**
- Header: album art (120dp) + album name + artist + year + track count
- Play All and Shuffle All buttons
- `LazyColumn` of `TrackListItem` ordered by disc/track number

---

### Screen 7: Artist Detail Screen (`ArtistDetailScreen.kt`)

**Layout:**
- Header: artist name (large), album count, track count
- Albums section: horizontal `LazyRow` of `AlbumCard`
- All tracks for artist (or navigate to each album)

---

### Screen 8: Playlist Screen (`PlaylistScreen.kt`)

**Layout:**
- Top bar: playlist name (editable inline), Play All button
- `LazyColumn` of tracks in playlist order
- Drag handles for reordering (using `androidx-compose-reorderable` library)
- Swipe to remove from playlist

---

### Screen 9: Settings Screen (`SettingsScreen.kt`)

**Sections:**

**Audio:**
- Gapless Playback toggle
- Output Sample Rate: Auto | 44.1 kHz | 48 kHz | 96 kHz | 192 kHz | 384 kHz
- Crossfade duration: 0–10 seconds (slider)

**Volume:**
- Default startup volume level (uses `VolumeSlider` component)

**Appearance:**
- Theme: AMOLED Black | Dark | Light (segmented button)

**File Explorer:**
- Show hidden files toggle
- Default start path (text field, browse button)

**Library:**
- Scan now button
- Library paths list (add/remove scan folders)

**About:**
- App version
- Device audio capabilities (reads `AudioManager.getProperty(PROPERTY_OUTPUT_SAMPLE_RATE)` and `PROPERTY_OUTPUT_FRAMES_PER_BUFFER`)

---

## 4. Features — Detailed Specification

### 4.1 Playback Queue

- ExoPlayer playlist API manages the queue
- Add to queue: appends to end of current playlist
- Play next: inserts after current item
- Queue visible via a sheet from Now Playing screen
- Queue state persisted via DataStore (JSON list of track IDs)

### 4.2 Search

- Searches tracks (title, artist, album), albums, artists simultaneously
- Results grouped by category in a unified list
- Debounced as user types (300ms debounce with `distinctUntilChanged`)

### 4.3 Notification & Lock Screen Controls

- `MusicPlayerService` creates a persistent notification (MediaSession style)
- Controls: previous, play/pause, next, close
- Album art shown in notification
- Lock screen media controls via Android MediaSession

### 4.4 Audio Focus

- ExoPlayer handles audio focus automatically when `handleAudioFocus = true`
- Pauses on incoming call, phone call end → resumes
- Ducks (lowers volume) when other app plays audio briefly

### 4.5 Headphone Unplug Detection

- ExoPlayer `setHandleAudioBecomingNoisy(true)` — pauses automatically when headphones are unplugged

### 4.6 ReplayGain

- `MediaMetadataRetriever` reads REPLAYGAIN_TRACK_GAIN and REPLAYGAIN_ALBUM_GAIN tags
- Values stored in `Track.replayGainTrack` and `Track.replayGainAlbum`
- Applied by adjusting `ExoPlayer.setVolume()` gain multiplicatively with user volume

### 4.7 Sleep Timer

- Sheet accessible from Now Playing → stops playback after N minutes
- Options: 15, 30, 45, 60, 90 minutes, or at end of current track
- Countdown timer shown in top bar of Now Playing while active

---

## 5. Audio Engine

### ExoPlayer Configuration

```kotlin
val exoPlayer = ExoPlayer.Builder(context)
    .setAudioAttributes(
        AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build(),
        true  // handle audio focus
    )
    .setHandleAudioBecomingNoisy(true)
    .build()
```

### Hi-Res Audio Path

The S24 Ultra DAC (Qualcomm Aqstic WCD9395) supports 32-bit / 384kHz. To use it:

1. Check device capability:
```kotlin
val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
val nativeRate = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
```

2. ExoPlayer selects hardware decoders via `MediaCodec` automatically for supported formats.

3. For bit-perfect output, use `AudioTrack` directly with `ENCODING_PCM_32BIT` or `ENCODING_PCM_FLOAT`. This bypasses Android's software mixer when the output device supports the format natively.

### Supported Formats

| Format | Extension | Decoder | Notes |
|--------|-----------|---------|-------|
| MP3 | .mp3 | MediaCodec | Native |
| AAC/M4A | .m4a, .aac | MediaCodec | Native |
| FLAC | .flac | MediaCodec | Native, up to 32-bit/192kHz |
| WAV | .wav | MediaCodec | Native PCM |
| OGG Vorbis | .ogg | MediaCodec | Native |
| OPUS | .opus | MediaCodec | Native |
| APE | .ape | FFmpeg extension | Requires NDK build — Session 14 |
| DSD | .dsf, .dff | FFmpeg extension | Requires NDK build — Session 14 |
| WMA | .wma | MediaCodec | Samsung includes codec |
| ALAC | .m4a (alac) | MediaCodec | Samsung includes codec |
| AIFF | .aif, .aiff | MediaCodec | PCM |
| MKA | .mka | MediaCodec | Matroska container |

---

## 6. Equalizer & Audio Effects

### How It Works

Android's `android.media.audiofx.Equalizer` class communicates directly with the device's hardware DSP via the audio HAL. On the S24 Ultra, this reaches the Qualcomm Aqstic WCD9395 codec.

### Initialization

```kotlin
// Must be called after ExoPlayer audio session ID is available
val sessionId = exoPlayer.audioSessionId  // non-zero when player is ready
val equalizer = Equalizer(0, sessionId)
equalizer.enabled = true

// Query hardware capabilities
val bandCount = equalizer.numberOfBands       // typically 5
val levelRange = equalizer.bandLevelRange     // e.g. [-1500, 1500] millibels
val frequencies = (0 until bandCount).map {
    equalizer.getBandFreqRange(it.toShort())  // returns [minHz, maxHz] in milliHz
}
```

### Setting Gain

```kotlin
// band: 0 to (numberOfBands-1)
// gainMb: e.g. 500 = +5.0 dB, -1000 = -10.0 dB
equalizer.setBandLevel(band.toShort(), gainMb.toShort())
```

### Built-in Preset Definitions (millibels)

All arrays are [Band0, Band1, Band2, Band3, Band4] for 5-band EQ.

| Preset | 60Hz | 230Hz | 910Hz | 3.6kHz | 14kHz |
|--------|------|-------|-------|--------|-------|
| Flat | 0 | 0 | 0 | 0 | 0 |
| Bass Boost | 600 | 400 | 0 | -100 | -200 |
| Treble Boost | -200 | -100 | 0 | 400 | 600 |
| Vocal Clarity | -300 | 0 | 500 | 400 | 0 |
| Rock | 400 | 200 | -100 | 200 | 400 |
| Classical | 0 | 0 | 0 | -200 | -400 |
| Electronic | 400 | 300 | -200 | 300 | 400 |

---

## 7. Volume Control

### Design

Android's system volume has ~15 steps for media volume — too coarse for audiophile use. The solution: fix system volume at 80% and control the remaining gain via ExoPlayer's software gain stage.

### Mathematics

50 levels spanning approximately -40 dB to 0 dB (reference):

```
gain = 10 ^ ((level - 50) × 0.04)
```

| Level | Gain Factor | dB |
|-------|-------------|-----|
| 50 | 1.0 | 0 dB |
| 45 | 0.631 | -4 dB |
| 40 | 0.398 | -8 dB |
| 35 | 0.251 | -12 dB |
| 25 | 0.100 | -20 dB |
| 10 | 0.016 | -36 dB |
| 1 | 0.010 | -40 dB |

### Implementation

```kotlin
// VolumeUtil.kt
fun levelToGain(level: Int): Float = 10f.pow((level - 50) * 0.04f)

// Apply to ExoPlayer
exoPlayer.volume = VolumeUtil.levelToGain(level)
```

### ReplayGain Integration

When ReplayGain tags are present, multiply the gains:

```kotlin
val replayGainFactor = 10f.pow(track.replayGainTrack / 20f)  // dB → linear
exoPlayer.volume = VolumeUtil.levelToGain(level) * replayGainFactor
```

---

## 8. File Explorer

### Permissions

```xml
<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" />
```

Request at runtime with a rationale dialog explaining the feature.

### File Operations

All operations run on `Dispatchers.IO` with `Result<T>` return type.

| Operation | API Used |
|-----------|----------|
| List directory | `File.listFiles()` |
| Rename | `File.renameTo()` |
| Delete | `File.deleteRecursively()` |
| Copy | `File.copyTo()` |
| Move | `copyTo()` + `delete()` |
| Create folder | `File.mkdirs()` |

### Metadata Editor

Uses `JAudioTagger` library (`net.jthink:jaudiotagger`):

```kotlin
val audioFile = AudioFileIO.read(File(path))
val tag = audioFile.tag
tag.setField(FieldKey.TITLE, "New Title")
tag.setField(FieldKey.ARTIST, "New Artist")
audioFile.commit()
```

Supported tag formats: ID3v1, ID3v2 (MP3), Vorbis Comment (FLAC/OGG), MP4 tags (M4A/ALAC).

---

## 9. Music Library

### Scanning Process

1. `WorkManager` triggers `LibraryScanWorker` on first launch and on user request
2. `MediaStoreScanner.scan()` queries `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI`
3. For each file, `MediaMetadataRetriever` extracts sample rate and bit depth
4. Results are upserted into Room database
5. Stale entries (files deleted from storage) are removed

### Auto-detection of New Files

```kotlin
contentResolver.registerContentObserver(
    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
    true,
    object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) { triggerRescan() }
    }
)
```

### Album Art

Album art is accessed via `ContentUris`:
```kotlin
val artUri = ContentUris.withAppendedId(
    Uri.parse("content://media/external/audio/albumart"),
    albumId
)
```
Coil handles loading and caching this URI in `AsyncImage`.

---

## 10. Database Schema

### Tables

**tracks**
```
id LONG PK, title TEXT, artist TEXT, albumName TEXT, albumId LONG,
artistId LONG, duration LONG, filePath TEXT, mimeType TEXT,
sampleRate INT, bitDepth INT, bitrate INT, fileSize LONG,
trackNumber INT, discNumber INT, year INT, genre TEXT,
dateAdded LONG, dateModified LONG, playCount INT, lastPlayed LONG,
isFavorite BOOL, replayGainTrack FLOAT, replayGainAlbum FLOAT
```

**albums**
```
id LONG PK, name TEXT, artist TEXT, artistId LONG,
trackCount INT, year INT, albumArtUri TEXT, genre TEXT
```

**artists**
```
id LONG PK, name TEXT, albumCount INT, trackCount INT
```

**playlists**
```
id LONG PK (autoincrement), name TEXT, description TEXT,
trackIds TEXT (JSON), createdAt LONG, modifiedAt LONG, coverArtTrackId LONG?
```

**eq_presets**
```
id LONG PK (autoincrement), name TEXT, bandGains TEXT (JSON),
bassBoostStrength INT, virtualizerStrength INT,
loudnessGainMb INT, isBuiltIn BOOL
```

---

## 11. Permissions

| Permission | When Requested | Purpose |
|------------|---------------|---------|
| `READ_MEDIA_AUDIO` | On first launch | Read audio files (Android 13+) |
| `MANAGE_EXTERNAL_STORAGE` | When File Explorer is opened | Full file system access |
| `POST_NOTIFICATIONS` | On first launch | Playback notification (Android 13+) |
| `FOREGROUND_SERVICE` | Declared, auto-granted | Run player service |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Declared, auto-granted | Media foreground service |
| `WAKE_LOCK` | Declared, auto-granted | Prevent CPU sleep during playback |
| `RECEIVE_BOOT_COMPLETED` | Declared, auto-granted | Session restore after reboot |

---

## 12. Dependencies

```kotlin
// gradle/libs.versions.toml
media3 = "1.5.1"        // ExoPlayer / MediaSession
room = "2.7.0"           // Local database
hilt = "2.54"            // Dependency injection
datastore = "1.1.2"      // Preferences persistence
coil = "2.7.0"           // Album art image loading
jaudiotagger = "3.0.1"   // Metadata editing
```

Full list: see `gradle/libs.versions.toml` and `app/build.gradle.kts`

---

## 13. Development Roadmap

Work through these sessions in order. Each is a Cursor AI session.

### Session 1: Project Scaffold ✅ (this setup)
- Build files, manifest, directory structure, theme

### Session 2: Room Database & Models
- Verify all entities, DAOs, AppDatabase compile
- Insert test data to verify Room setup
- **Test:** create a playlist, insert tracks, query them back

### Session 3: MediaStore Scanner
- `MediaStoreScanner.scan()` running and returning data
- `MusicRepository.scanLibrary()` upserts to Room
- **Test:** run scan on device with music files, verify tracks appear in DB

### Session 4: MediaSessionService + ExoPlayer
- `MusicPlayerService` starts correctly
- ExoPlayer plays a track from file path
- Notification shows with controls
- Background playback works (lock screen, screen off)
- **Test:** play a FLAC file end-to-end

### Session 5: Library Screen
- Tracks, Albums, Artists, Playlists tabs working
- Data flows from Room via ViewModel to Compose UI
- **Test:** all tabs show correct data from scan

### Session 6: Now Playing Screen
- Service connection: UI binds to `MusicPlayerService`
- Play/pause/next/previous work via MediaController
- Progress bar updates in real-time
- **Test:** full playback flow from Library → Now Playing

### Session 7: 50-Level Volume Control
- `VolumeSlider` component wired to service
- Volume persisted in DataStore
- dB readout correct for each level
- **Test:** verify level 35 corresponds to ~-12 dB

### Session 8: Equalizer Screen
- Service connection reads hardware bands
- Sliders move and `setBandLevel()` is called immediately
- Preset save/load works
- **Test:** apply Bass Boost preset, verify audible change

### Session 9: File Explorer Screen
- Browse filesystem, list/navigate directories
- Copy/move/rename/delete operations work
- `MANAGE_EXTERNAL_STORAGE` permission flow
- **Test:** move a music file to another folder, verify it's reflected

### Session 10: Metadata Editor
- Tap "Edit tags" in file explorer context menu
- Edit title/artist/album/year, save
- `JAudioTagger` writes back to file
- Library re-scan picks up changes
- **Test:** edit a FLAC file's title, re-scan, verify Library shows new title

### Session 11: Settings Screen
- All preferences persist and reload
- Theme switching (AMOLED/Dark/Light) applies immediately
- Sample rate force setting plumbed to AudioTrack output format
- **Test:** change theme, reopen app, verify setting remembered

### Session 12: Search
- Cross-library search working in Library screen
- 300ms debounce implemented
- Results show in all relevant tabs
- **Test:** search for a partial artist name

### Session 13: Playlists
- Create/rename/delete playlists
- Add tracks from Library with track picker
- Drag-to-reorder in Playlist screen
- **Test:** create playlist, add 5 tracks, reorder, play from position 3

### Session 14: Hi-Res Audio Path Tuning
- Format badge shows correct info from `MediaMetadataRetriever`
- FLAC/WAV 24-bit files identified correctly
- Device capability shown in Settings → About
- **Test:** play a 24-bit/96kHz FLAC, verify badge shows "FLAC 24-bit / 96 kHz"

### Session 15: Polish & Edge Cases
- AMOLED black theme refined (true blacks on all backgrounds)
- Edge-to-edge layout with status/nav bar insets
- Empty states for all screens
- Error dialogs (file not found, permission denied)
- Animated transitions between screens

---

## 14. Samsung S24 Ultra Hardware Notes

### Audio Codec
**Qualcomm Aqstic WCD9395** — the hardware DAC/codec on the S24 Ultra.

- Supports 24-bit / 192kHz output natively over the phone speaker and headphone path
- Supports 32-bit / 384kHz when outputting to USB-C DAC/DAC adapters (USB Audio Class 2.0)
- Android API exposes 5 EQ bands for this codec via `android.media.audiofx.Equalizer`

### Check Device Capabilities at Runtime

```kotlin
val audioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
val nativeSampleRate = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)  // e.g. "48000"
val nativeBufferSize = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)
```

Display these in Settings → About so you know what the device is using.

### USB-C Audio Output

When a USB DAC is connected:
- Android routes audio via USB Audio HAL
- The connected DAC determines max sample rate and bit depth
- No special code needed — Android handles device switching automatically
- The format badge in Now Playing reflects the actual output format being decoded

### One UI Audio Stack

Samsung's One UI adds its own audio processing layer on top of Android. The `Equalizer` AudioFX API goes through this stack. Some Samsung-specific effects (like Dolby Atmos when enabled in system settings) may interact with the app EQ. Recommend in the Settings UI to note: "For best results, disable system EQ in Sound settings."

---

*Document generated as part of project setup. Update as features are implemented.*
