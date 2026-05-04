# UMAK — Session Prompts

Use each prompt below at the start of a fresh Cursor Agent chat.
Sessions build on each other — complete them in order.

The full technical spec for every feature is in `SPEC.md` at the project root.

---

## Session 2 — Room Database Verification

```
We are building a HiFi music player Android app (Kotlin + Jetpack Compose + Hilt + Room).
The project is at: C:\Users\udays\OneDrive\Documents\Work\Android_Apps\MusicPlayer
The full spec is in SPEC.md at the project root.

Session 2 goal: verify and complete the Room database layer.
- Review all 5 Room entities (Track, Album, Artist, Playlist, EqPreset) and all DAOs for correctness
- Seed the database with the 7 built-in EQ presets (Flat, Bass Boost, Treble Boost, Vocal Clarity, Rock, Classical, Electronic) on first app launch via a Room RoomDatabase.Callback
- Add anything missing to the Hilt DI module in di/AppModule.kt
- Ensure the app builds and launches on the Samsung Galaxy S24 Ultra without crashing
- Test: add a temporary debug log that prints track count from DB on app start to confirm Room is working
```

---

## Session 3 — MediaStore Scanner

```
We are building a HiFi music player Android app (Kotlin + Jetpack Compose + Hilt + Room).
The project is at: C:\Users\udays\OneDrive\Documents\Work\Android_Apps\MusicPlayer
The full spec is in SPEC.md at the project root. Sessions 1 and 2 are complete.

Session 3 goal: get the music library scanner working end-to-end.
- Wire READ_MEDIA_AUDIO and POST_NOTIFICATIONS permission requests on first launch using Accompanist Permissions (already a dependency)
- Implement LibraryScanWorker (WorkManager) that calls MusicRepository.scanLibrary()
- Trigger the scan automatically on first launch and show a progress indicator on the Home screen
- After scan, HomeScreen should show real recently-added albums from Room
- The Home Screen Rescan button should trigger a manual re-scan
- Test: install on Samsung Galaxy S24 Ultra (Android 16), grant permissions, verify real music files appear in the app
```

---

## Session 4 — ExoPlayer + MediaSessionService

```
We are building a HiFi music player Android app (Kotlin + Jetpack Compose + Hilt + Room).
The project is at: C:\Users\udays\OneDrive\Documents\Work\Android_Apps\MusicPlayer
The full spec is in SPEC.md at the project root. Sessions 1–3 are complete.

Session 4 goal: wire up audio playback end-to-end.
- Complete MusicPlayerService so ExoPlayer starts, plays a track from its file path, and transport controls work
- Create a PlayerController Hilt singleton that wraps MediaController so all ViewModels can control playback without knowing about the service directly
- Wire play/pause/next/previous in NowPlayingViewModel to PlayerController
- Show the persistent playback notification (Media3 MediaSession style) with album art and transport controls
- Attach hardware Equalizer, BassBoost, Virtualizer, and LoudnessEnhancer to the audio session once ExoPlayer emits a non-zero audioSessionId
- The device is Samsung Galaxy S24 Ultra running Android 16 (API 36) — target SDK in build.gradle.kts is 35, that is correct and compatible
- Test: tap a track in the Library, verify it plays on device with notification visible on the lock screen
```

---

## Session 5 — Library Screen

```
We are building a HiFi music player Android app (Kotlin + Jetpack Compose + Hilt + Room).
The project is at: C:\Users\udays\OneDrive\Documents\Work\Android_Apps\MusicPlayer
The full spec is in SPEC.md at the project root. Sessions 1–4 are complete.

Session 5 goal: make the Library screen fully functional.
- All 4 tabs (Tracks, Albums, Artists, Playlists) display real data flowing from Room → Repository → ViewModel → Compose UI
- Tapping a track starts playback via PlayerController and navigates to Now Playing
- Tapping an album navigates to Album Detail with the correct track list ordered by disc/track number
- Tapping an artist navigates to Artist Detail showing their albums
- The MiniPlayer bar appears at the bottom of Library/Home screens when something is playing and navigates to Now Playing on tap
- Add a bottom navigation bar so the user can switch between Home and Library
- Test: browse full music library on device, verify all data is correct and navigation works
```

---

## Session 6 — Now Playing Screen

```
We are building a HiFi music player Android app (Kotlin + Jetpack Compose + Hilt + Room).
The project is at: C:\Users\udays\OneDrive\Documents\Work\Android_Apps\MusicPlayer
The full spec is in SPEC.md at the project root. Sessions 1–5 are complete.

Session 6 goal: complete the Now Playing screen.
- Progress scrubber updates in real time as the track plays (polling ExoPlayer position via a coroutine flow)
- Seeking (dragging the scrubber) works and seeks ExoPlayer
- Track title, artist, and album art update automatically when the track changes
- Shuffle toggle and Repeat cycle (Off → One → All) are wired and persist their state
- Format badge (e.g. "FLAC 24-bit / 96 kHz") reads from the Track entity in Room and renders correctly using AudioFormatUtil
- The 50-level VolumeSlider is wired to ExoPlayer.setVolume() via VolumeUtil.levelToGain(); volume level persists in DataStore across sessions
- Favorite heart icon toggles isFavorite in the database
- Test: play several tracks, seek forward, toggle shuffle, change repeat mode — all controls respond immediately and correctly
```

---

## Session 7 — Hardware Equalizer Screen

```
We are building a HiFi music player Android app (Kotlin + Jetpack Compose + Hilt + Room).
The project is at: C:\Users\udays\OneDrive\Documents\Work\Android_Apps\MusicPlayer
The full spec is in SPEC.md at the project root. Sessions 1–6 are complete.

Session 7 goal: complete the hardware Equalizer screen.
- EqualizerViewModel connects to the Equalizer instance held by MusicPlayerService and reads the actual hardware band count and frequency range (typically 5 bands on the S24 Ultra's Qualcomm Aqstic WCD9395 codec)
- Each band slider calls equalizer.setBandLevel() immediately on change — the effect must be heard in real time while music is playing
- BassBoost, Virtualizer, and LoudnessEnhancer sliders are wired to their respective AudioFX objects in the service
- The 7 built-in EQ presets seeded in Session 2 appear as filter chips; applying one sets all sliders and AudioFX values immediately
- User can save current settings as a named custom preset stored in Room; custom presets can be deleted
- EQ on/off toggle disables all effects without losing the current settings
- EQ state (last used preset or custom settings) persists across app restarts via DataStore
- Test: play music, open EQ, boost the lowest band by +10 dB — must be audibly different. Apply "Flat" — must return to neutral.
```

---

## Session 8 — File Explorer & Metadata Editor

```
We are building a HiFi music player Android app (Kotlin + Jetpack Compose + Hilt + Room).
The project is at: C:\Users\udays\OneDrive\Documents\Work\Android_Apps\MusicPlayer
The full spec is in SPEC.md at the project root. Sessions 1–7 are complete.

Session 8 goal: complete the File Explorer screen and add an in-app metadata editor.
- On first open, request MANAGE_EXTERNAL_STORAGE with a clear rationale dialog explaining it is needed for file management
- Directory browsing and breadcrumb navigation work correctly from the storage root
- Long-press enters multi-select mode; Cut / Copy / Paste / Delete / Rename all work with correct error handling
- New Folder creation works
- Tapping an audio file plays it immediately via PlayerController
- The "Edit Tags" option in the file context menu opens a bottom sheet with editable fields: Title, Artist, Album, Year, Track Number — uses JAudioTagger (already a dependency) to write tags back to the file
- After saving tags, trigger a library re-scan so the Library screen reflects the changes
- Test: move a folder of albums to a new location; edit the title tag of a FLAC file; verify the Library shows the new title after re-scan
```

---

## Session 9 — Settings, Search & Playlists

```
We are building a HiFi music player Android app (Kotlin + Jetpack Compose + Hilt + Room).
The project is at: C:\Users\udays\OneDrive\Documents\Work\Android_Apps\MusicPlayer
The full spec is in SPEC.md at the project root. Sessions 1–8 are complete.

Session 9 goal: complete Settings, cross-library Search, and Playlist management.

Settings:
- All preferences (theme, gapless, crossfade, default volume, sample rate, show hidden files) persist in DataStore and take effect immediately
- Theme switching (AMOLED Black / Dark / Light) applies without restarting the app
- Settings → About section reads and displays device audio capabilities using AudioManager.getProperty()

Search:
- The search bar in the Library screen queries tracks, albums, and artists simultaneously with a 300ms debounce
- Results update as the user types

Playlists:
- Create new playlist (FAB on Playlists tab), rename inline, delete with confirmation
- Add tracks to a playlist from a track's more-options menu or from a track picker sheet
- Playlist screen shows tracks in order with a drag handle to reorder
- Swipe a track in the playlist to remove it

Test: create a playlist, add 5 tracks, reorder them, play from position 3. Search for a partial artist name and verify results appear.
```

---

## Session 10 — Polish & Final Testing

```
We are building a HiFi music player Android app (Kotlin + Jetpack Compose + Hilt + Room).
The project is at: C:\Users\udays\OneDrive\Documents\Work\Android_Apps\MusicPlayer
The full spec is in SPEC.md at the project root. Sessions 1–9 are complete.

Session 10 goal: polish the app and fix any remaining issues.
- Ensure true AMOLED black (Color(0xFF000000)) is used consistently in the AmoledBlack theme — no dark grays on list backgrounds
- Apply edge-to-edge layout with proper WindowInsets padding on all screens so content is not hidden behind the status bar or navigation bar
- Add meaningful empty states to all screens (e.g. "No music found — tap Scan" on Home when library is empty)
- Add error handling dialogs for: file not found during playback, permission denied in File Explorer, tag write failure in metadata editor
- Animated screen transitions using Compose Navigation's built-in animations
- Sleep timer: implement the timer sheet accessible from the Now Playing screen (15/30/45/60/90 min or end of track options)
- ReplayGain: read REPLAYGAIN_TRACK_GAIN tag and apply it as a gain multiplier on top of the user volume level
- Final test pass: play MP3, FLAC (24-bit/96kHz), WAV, M4A files — verify format badge is correct for each, EQ and volume work, file explorer can reorganize files, playlists persist after killing the app
```
