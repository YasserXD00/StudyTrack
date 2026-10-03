# StudyTrack 📚🚀

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84.svg?style=flat&logo=android)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.21-7F52FF.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Room](https://img.shields.io/badge/Database-Room%20SQLite-FFCA28.svg?style=flat)](https://developer.android.com/training/data-storage/room)
[![Release](https://img.shields.io/github/v/release/YasserXD00/StudyTrack?color=blue)](https://github.com/YasserXD00/StudyTrack/releases)

> **StudyTrack** is a modern, all-in-one student productivity and study management application built with **Jetpack Compose** and **Material 3**. Designed to help students stay organized, build positive study habits, track expenses, learn vocabulary, and gamify their learning journey.

---

## 📥 Download the App

You can download the ready-to-install Android APK directly from GitHub Releases:

👉 **[Download Latest APK from Releases](https://github.com/YasserXD00/StudyTrack/releases)**

1. Download `StudyTrack-v1.0.apk` onto your Android phone.
2. Open the downloaded file to install (allow "Install from Unknown Sources" if prompted).
3. Start fresh with your own custom profile, goals, and schedule!

---

## ✨ Features

- **🏠 Modern Dashboard**: Real-time overview of daily study targets, active streak, pending tasks, recent habits, and quick-action shortcuts.
- **⏱️ Pomodoro Study Timer**: Built-in focus timer with presets (25/5m, 50/10m, and custom intervals), sound alerts, and focus session history.
- **📝 Task Management**: Create, categorize, and prioritize assignments and tasks with due dates and tags.
- **🎯 Milestone Goal Tracking**: Break large academic goals into actionable checkpoints with progress bars.
- **🌱 Habit Tracker**: Daily habit checkboxes with streak calculation and consistency metrics.
- **📚 Reading Tracker**: Keep track of books, reading progress, and page counts.
- **🇬🇧 English Vocabulary Builder**: Built-in flashcards and vocabulary memorization tool.
- **💰 Student Expense Tracker**: Manage student budgets, log expenses across categories, and switch between major currencies (`$`, `€`, `£`, `MAD`, `SAR`, `DZD`, etc.).
- **🏆 Gamification & Achievements**: Earn XP, level up, and unlock achievements for studying and staying consistent.
- **🌍 Multi-Language Support**: Full localization for 7 languages including RTL support:
  - 🇺🇸 English
  - 🇸🇦 Arabic (العربية - RTL)
  - 🇫🇷 French (Français)
  - 🇩🇪 German (Deutsch)
  - 🇪🇸 Spanish (Español)
  - 🇷🇺 Russian (Русский)
  - 🇨🇳 Chinese (中文)
  - 🇯🇵 Japanese (日本語)
- **🌓 Dark Mode Support**: Sleek, eye-friendly theme with dynamic Material 3 colors.
- **🔒 100% Offline & Private**: All student data is stored locally on-device using Room Database.

---

## 🛠️ Tech Stack & Architecture

- **Architecture**: Clean MVVM (Model-View-ViewModel) with Single Source of Truth repository pattern.
- **UI Framework**: Modern Jetpack Compose with Material 3 components and custom animations.
- **Local Database**: Room Database (SQLite) with Kotlin Coroutines and asynchronous `Flow`.
- **Dependency & Build**: Gradle 9.3.1 with Kotlin DSL and Version Catalog (`libs.versions.toml`).
- **Language**: 100% Kotlin with type safety.

---

## 🚀 Building & Running Locally

### Prerequisites
- [Android Studio Ladybug / Meerkat or newer](https://developer.android.com/studio)
- JDK 17 or JDK 21+
- Android SDK 35+

### Steps
1. Clone the repository:
   ```bash
   git clone https://github.com/YasserXD00/StudyTrack.git
   cd StudyTrack
   ```

2. Open the project in Android Studio.

3. Build the debug or release APK via command line:
   ```bash
   # Build debug APK
   ./gradlew assembleDebug

   # Build release APK
   ./gradlew assembleRelease
   ```
   The APK will be generated in `app/build/outputs/apk/release/`.

---

## 📄 License

This project is open-source and part of my portfolio. Feel free to use and learn from it!
