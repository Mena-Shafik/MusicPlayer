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
