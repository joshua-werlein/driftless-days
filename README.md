# Driftless Days
 
> *Days worth looking at.*
 
A native Android calendar app built around Wisconsin nature photography. Every date maps to a unique photo from the Driftless Region — a rolling, glacier-free landscape of bluffs, rivers, and light that changes with the seasons. The same date always shows the same photo, making your home screen feel less like a wallpaper and more like a record of time.
 
---
 
## What It Does
 
**Live Wallpaper** — Full-screen Wisconsin nature photography as your home screen background. A parallax effect shifts the photo as you swipe between pages, adding depth to the experience.
 
**Home Screen Widgets** — Three sizes to fit how you use your phone:
- 2×2 compact: date and a thumbnail
- 4×2 photo strip: landscape photo with today's date
- 4×4 calendar grid: monthly view with a photo header
**Google Calendar Integration** — Your events overlay directly on the photo background. All calendars sync — personal, work, shared — displayed cleanly without leaving the home screen.
 
**Date-Consistent Photo Mapping** — The core idea. A deterministic hash maps each calendar date to a specific photo. The same date always returns the same image, regardless of when you open the app or what you've added to your library. January 14th is always January 14th.
 
**Personal Photo Category** *(coming soon)* — Upload your own photos. Birthdays, trips, places that matter to you. Your actual life as your actual home screen.
 
---
 
## Architecture
 
```
Android App (Kotlin)
├── CalendarScreen        Compose full-screen UI with photo background
├── CalendarRepository    Google Calendar API, all calendars via OAuth
├── PhotoRepository       Date → Worker URL mapping
├── Widget System         Jetpack Glance (2×2, 4×2, 4×4)
└── WallpaperService      Live wallpaper with parallax via WallpaperManager
 
Cloudflare
├── Worker                Date-hash photo selection, R2 serving
└── R2                    Photo storage (1080p, 80% JPEG)
```
 
**Stack:** Kotlin · Jetpack Compose · Jetpack Glance · Coil · OkHttp · Google Calendar API · Cloudflare Workers · Cloudflare R2
 
---
 
## Features
 
| Feature | Status |
|---|---|
| Live wallpaper with daily photo | ✅ Working |
| Parallax effect on home screen | 🔧 In progress |
| Google Calendar sync | ✅ Working |
| Home screen widgets (3 sizes) | ✅ Working |
| CalendarScreen in-app view | 🔧 In progress |
| Personal photo uploads | 📋 Planned |
| Microsoft 365 calendar | 📋 Planned |
| Moon phases | 📋 Planned |
| Packers schedule | 📋 Planned |
 
---
 
## Photo Backend
 
Photos are stored in Cloudflare R2 and served via a Cloudflare Worker. The Worker accepts a date parameter and applies a deterministic hash to select a photo — ensuring the same date always returns the same image across all devices and sessions.
 
The Worker endpoint:
```
https://driftless-worker.jjwerlein.workers.dev/photo/nature/YYYY-MM-DD
```
 
Images are compressed to 1080p at 80% JPEG quality at upload time.
 
---
 
## Getting Started
 
### Prerequisites
- Android Studio Ladybug or later
- Android device or emulator running API 27+
- Google Cloud project with Calendar API enabled
- Cloudflare account with R2 and Workers
### Setup
1. Clone the repository
2. Add your Google OAuth Web Client ID to `local.properties`:
   ```
   GOOGLE_WEB_CLIENT_ID=your_client_id_here
   ```
3. Deploy the Cloudflare Worker from `/driftless-worker`
4. Build and run via Android Studio
---
 
## The Region
 
The [Driftless Area](https://en.wikipedia.org/wiki/Driftless_Area) is a region of southwestern Wisconsin (and parts of Minnesota, Iowa, and Illinois) that was bypassed by glaciers during the last ice age. The result is a landscape of steep bluffs, spring-fed streams, coulees, and ridgelines unlike anything else in the upper Midwest. It's where this app is from.
 
---
 
## License
 
Apache 2.0 — see [LICENSE](LICENSE) for details.
 
---
 
## Author
 
[Joshua Werlein](https://github.com/joshua-werlein) — freelance developer based in Wisconsin.
