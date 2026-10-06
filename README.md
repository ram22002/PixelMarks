# PixelMarks

Free and open-source, privacy-first, local-first bookmark manager for Android.

## Status

Early development — Task 1: Project foundation, architecture skeleton & development environment.

## Technology

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose
- **Design System:** Material 3
- **Architecture:** Clean Architecture (MVVM + Repository + Local-First)
- **Minimum SDK:** Android 7.0 (API 24)
- **Target / Compile SDK:** Android 15 (API 35)

## Feature Implementation Roadmap

- [x] Project Foundation & Architecture Skeleton (Task 1)
- [ ] Room Database & Schema (Task 2 - Upcoming)
- [ ] Domain Models & Repository (Task 3 - Planned)
- [ ] Share Target Ingestion (Task 4 - Planned)
- [ ] Metadata Scraper Engine (Task 5 - Planned)
- [ ] Core Compose UI & Views (Task 6 - Planned)
- [ ] Search & Filters (Task 7 - Planned)
- [ ] Tagging & Collections (Task 8 - Planned)
- [ ] Reminders & Alarms (Task 9 - Planned)
- [ ] Local Backup & Encrypted Export (Task 10 - Planned)
- [ ] Local PC Companion Web Client (Task 11 - Planned)
- [ ] Hardening & Polishing (Task 12 - Planned)

## Architecture Overview

```text
Compose UI
   ↓
ViewModel
   ↓
Domain Use Cases
   ↓
Repositories
   ↓
Room Database (Single Source of Truth)
```

No remote accounts, no cloud database, no tracking, 100% offline-first.

## License

Licensed under the [Apache License, Version 2.0](LICENSE).
