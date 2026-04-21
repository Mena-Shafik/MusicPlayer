# 🎵 MusicPlayer

A modern **Android music player application** built with **Kotlin** and **Jetpack Compose**, supporting **local music playback**, **radio streaming**, and **background audio playback** using a **foreground service**.

This project demonstrates real-world Android development practices, including **MVVM architecture**, **Compose Navigation**, **repositories**, and **media playback services**.

---

## ✨ Features

- 🎶 Local music playback
- 📻 Online radio streaming
- ⏯️ Mini player & full player UI
- 🔔 Foreground playback service with media notifications
- 🔍 Song search
- 🎨 UI built entirely with Jetpack Compose
- 🧠 MVVM + Repository architecture
- ⚙️ Persistent user preferences
- 🧭 Navigation using Navigation Compose

---

## 🛠 Tech Stack

- **Language:** Kotlin  
- **UI:** Jetpack Compose  
- **Architecture:** MVVM + Repository  
- **Media Playback:** Android Media APIs  
- **Background Playback:** Foreground Service  
- **Navigation:** Navigation Compose  
- **Build System:** Gradle (Kotlin DSL)

---

## 📁 Project Structure
```
com.example.musicplayer
│
├── composable/ # Reusable Jetpack Compose UI components
│
├── model/ # Data models
│
├── music/ # Music player screen, ViewModel & intent helpers
│
├── navigation/ # App navigation graph & NavHost
│
├── preferences/ # SharedPreferences management
│
├── radio/ # Radio feature (API, service, ViewModel, repository)
│
├── service/ # Foreground playback service & notifications
│
├── songlist/ # Song list screen & ViewModel
│
├── ui/
│ ├── components/ # Shared UI helpers
│ └── theme/ # App theme (Color, Type, Theme)
│
├── MainActivity.kt # Application entry point
├── MainViewModel.kt # Shared app-level state
└── Util.kt # Utility helpers

```
---

## 🧠 Architecture Overview

The app follows **MVVM (Model–View–ViewModel)** principles:

### UI Layer
- Built using **Jetpack Compose**
- Stateless composables driven by ViewModel state

### ViewModels
- Contain business logic and UI state
- One ViewModel per feature (music player, song list, radio, etc.)

### Repositories
- Abstract data sources (local media, radio streams)
- Keep ViewModels clean and testable

### Services
- Foreground service ensures uninterrupted audio playback
- Media notifications allow background control

This structure keeps the codebase **scalable, maintainable, and production-ready**.

---

## ▶️ How to Run

### Prerequisites
- Android Studio (latest recommended)
- Android device or emulator
- JDK 8 or higher

### Steps

```bash
git clone https://github.com/Mena-Shafik/MusicPlayer.git


---

## 🖼️ Screenshots

Place your captured screenshots in `app/screenshots/`. Example filenames used by the project's test scripts and CI (if enabled):

- `app/screenshots/home.png` — Home / song list
- `app/screenshots/album.png` — Album/grouped view
- `app/screenshots/artist.png` — Artist/grouped view
- `app/screenshots/era.png` — Era/grouped view
- `app/screenshots/player.png` — Full player screen

Additional suggested screenshots:

- `app/screenshots/radio.png` — Radio screen / current station
- `app/screenshots/playlist.png` — Playlists list
- `app/screenshots/playlist_detail.png` — Playlist detail (songs in a playlist)

Embed screenshots in this README using relative paths so GitHub renders them. Example:

```markdown
## Screenshots

Home screen:

![Home screen](app/screenshots/home.png)

Player screen:

![Player screen](app/screenshots/player.png)
```

Tip: keep both high-resolution originals (PNG) and smaller web-optimized copies if you want the README to load faster.

---

## 📸 Clickable gallery

Click to expand the screenshots gallery. If images don't display, make sure the files exist at the paths listed above (they must be committed to the repository under `app/screenshots/`).

<details>
  <summary>Open screenshots gallery</summary>

  <div align="center">
    <a href="app/screenshots/home.png"><img src="app/screenshots/home.png" alt="Home" width="240" style="margin:6px;"/></a>
    <a href="app/screenshots/album.png"><img src="app/screenshots/album.png" alt="Album" width="240" style="margin:6px;"/></a>
    <a href="app/screenshots/artist.png"><img src="app/screenshots/artist.png" alt="Artist" width="240" style="margin:6px;"/></a>
    <a href="app/screenshots/era.png"><img src="app/screenshots/era.png" alt="Era" width="240" style="margin:6px;"/></a>
    <a href="app/screenshots/player.png"><img src="app/screenshots/player.png" alt="Player" width="240" style="margin:6px;"/></a>
    <a href="app/screenshots/radio.png"><img src="app/screenshots/radio.png" alt="Radio" width="240" style="margin:6px;"/></a>
    <a href="app/screenshots/playlist.png"><img src="app/screenshots/playlist.png" alt="Playlists" width="240" style="margin:6px;"/></a>
    <a href="app/screenshots/playlist_detail.png"><img src="app/screenshots/playlist_detail.png" alt="Playlist Detail" width="240" style="margin:6px;"/></a>
  </div>

</details>

---

## 📻 Radio flow

The app includes an integrated radio experience. Use these steps to try or capture the radio UI:

1. Open the app and navigate to the Radio tab (bottom navigation).
2. The app shows a list of radio stations (built-in defaults or fetched from Radio Browser).
3. Tap a station card to start playback. The app will start a foreground service and show a notification.
4. Use the mini-player or the full player screen to control playback (play/pause, stop, volume).
5. To capture the radio screen, take a screenshot after tapping a station — save it as `app/screenshots/radio.png`.

Notes:
- The radio list can be switched between default bundled stations and an online list using Settings.
- Playback runs in a foreground service so the audio continues when the app is backgrounded.

---

## ▶️ Playlist flow

Playlists let you group songs and play them as a list. Typical flows:

Creating and using playlists
1. Open the Playlists tab from the bottom navigation.
2. Tap the + / Add button to create a new playlist (give it a name).
3. Use "Add songs" to browse your library and add items to the playlist.
4. From the playlist detail screen tap a song to start playback of the playlist starting from that song.

Adding songs from lists
- Use the three-dot menu on any song row to "Add to Playlist". The dialog lets you pick the target playlist.
- When playing from a playlist, the app uses the playlist ordering for next/previous operations.

Capturing playlist UI
- `app/screenshots/playlist.png` — capture the Playlists list.
- `app/screenshots/playlist_detail.png` — capture the playlist detail that shows songs in the playlist.

---

