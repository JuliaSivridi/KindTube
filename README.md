# KindTube

![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-34A853?style=for-the-badge&logo=android&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![YouTube Data API](https://img.shields.io/badge/YouTube_Data_API_v3-FF0000?style=for-the-badge&logo=youtube&logoColor=white)

A whitelist YouTube player for kids. The child sees **only the channels a parent has approved** — no algorithmic feed, no recommendations, no comments, no ads UI. Built with Kotlin and Jetpack Compose.

---

## Why

YouTube Kids still recommends content by algorithm. KindTube flips the model: the parent curates a list of channels, and the app shows a feed built **exclusively** from those channels' uploads. Nothing else can appear.

---

## Features

**🧒 For the kid**
- Home feed assembled from approved channels only — random mix, pull-to-refresh for a new one
- Big, friendly player controls (YouTube Kids style): oversized play/pause, chunky seek bar
- Tap the video to show/hide controls and the "More videos" row; replay button when the video ends
- "More videos" suggestions come from the same approved channels — never from YouTube's algorithm
- Watch history tab
- Search — restricted to safe search and embeddable videos

**👨‍👩‍👧 For the parent**
- Curated channel list: add channels by search, remove anytime
- Block a channel right from the player (gated by a parental control check)
- Non-embeddable videos are filtered out automatically — the kid never hits "Video unavailable"
- Export / import the channel list as a JSON file — move your setup to another device or reinstall without losing it

**🎛 Player (custom, WebView-based)**
- YouTube's own UI is fully hidden — no channel links, no Subscribe buttons, no end-screen recommendations
- All navigation out of the app is blocked
- Playback controlled directly: play/pause, seek, replay, automatic end-of-video screen

---

## Tech Stack

| Layer | Technology |
|---|---|
| 🗣️ Language | Kotlin |
| 🎨 UI | Jetpack Compose (Material 3) |
| 🏗️ Architecture | Clean-ish: UI → ViewModel → UseCase → Repository |
| 💉 DI | Hilt |
| 🗄️ Local DB | Room (channels, feed, history, video cache) |
| 🌐 Network | Retrofit + OkHttp → YouTube Data API v3 |
| 🖼 Images | Coil |
| ▶️ Playback | WebView with the YouTube embed page (custom chrome-less player) |
| 📱 Min SDK | API 23 (Android 6.0) |

---

## Building

1. Clone the repository
   ```bash
   git clone git@github.com:JuliaSivridi/KindTube.git
   cd KindTube
   ```
2. Get a **YouTube Data API v3** key in [Google Cloud Console](https://console.cloud.google.com) (enable the API, create an API key)
3. Add the key to `local.properties`:
   ```properties
   youtubeApiKey=YOUR_API_KEY
   ```
4. Open in **Android Studio** and click **Run ▶**

### Release build (CI)

Pushing a `v*` tag triggers `.github/workflows/release.yml`, which builds a signed
APK + AAB and attaches them to a GitHub Release. Required repository secrets:
`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`, `YOUTUBE_API_KEY`.

```bash
git tag v1.0.0
git push origin v1.0.0
```

---

## Documentation

- **Technical specification:** [`docs/tech-spec.md`](docs/tech-spec.md)

---

## How it works

1. The parent adds channels in the **Subscriptions** tab (search by name)
2. The feed samples up to 12 random channels and picks 3–5 recent videos from each, shuffled
3. The player loads the YouTube embed page in a WebView and injects CSS that hides
   every piece of YouTube chrome; playback is driven directly through the `<video>` element
4. Videos with embedding disabled by their owner are filtered out at fetch time
   (`status.embeddable`), so everything shown is guaranteed to play
