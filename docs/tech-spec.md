# KindTube — Technical Specification

**Version:** 1.1 (versionCode 2)
**Repository:** https://github.com/JuliaSivridi/KindTube
**Last updated:** 2026-06-12

---

## Table of Contents

1. [Overview](#1-overview)
2. [Tech Stack](#2-tech-stack)
3. [Architecture](#3-architecture)
4. [Package / Folder Structure](#4-package--folder-structure)
5. [Data Model](#5-data-model)
6. [Database Schema (Room)](#6-database-schema-room)
7. [First-Launch Setup](#7-first-launch-setup)
8. [YouTube Data API Layer](#8-youtube-data-api-layer)
9. [UI Screens](#9-ui-screens)
10. [Key Components](#10-key-components)
11. [Theme & Colors](#11-theme--colors)
12. [Navigation](#12-navigation)
13. [Loading & Empty States](#13-loading--empty-states)
14. [CI/CD & Build](#14-cicd--build)
15. [First-Time Setup (New Developer)](#15-first-time-setup-new-developer)
16. [Key Algorithms](#16-key-algorithms)

---

## 1. Overview

KindTube is a **whitelist YouTube player for kids** (Android, landscape-only tablet UI).
The parent curates a list of channels; the child sees a feed built exclusively from those
channels' uploads. There is no algorithmic recommendation surface anywhere in the app.

Key design decisions:

- **No Google sign-in.** An early prototype used a Google account + the user's real YouTube
  subscriptions; this was abandoned. The channel list lives entirely in a local Room DB and
  is portable via JSON export/import (v1.1). The `google-signin` / `security-crypto`
  dependencies still listed in the version catalog are leftovers of that prototype and are
  **not** wired into the build.
- **Custom chrome-less player.** The YouTube embed page is loaded as the *main frame* of a
  WebView (with a `Referer` header — without it YouTube rejects the context with error 153).
  Because the page is the main frame, its DOM is same-origin: injected CSS hides every piece
  of YouTube UI and playback is driven directly through the `<video>` element. See §16.
- **All YouTube navigation blocked.** `shouldOverrideUrlLoading` returns `true` for every
  request and `onCreateWindow` returns `false`, so links, popups and the "Watch on YouTube"
  affordances are dead. A Compose touch-interceptor additionally covers the whole WebView.
- **Parental control** is a math gate (multiplication, 2–9), not a password — guards
  Settings entry, channel removal, and in-player channel blocking.
- **Non-embeddable videos are filtered out** at fetch time (`status.embeddable == false`),
  so the child never lands on a "Video unavailable" screen.

The spec covers the app as of v1.1 (channel-list JSON backup added).

---

## 2. Tech Stack

| Layer | Library | Version | Notes |
|---|---|---|---|
| Language | Kotlin | 2.2.10 | AGP 9.1.0, KSP 2.3.6 |
| UI | Jetpack Compose (Material 3) | BOM 2024.09.00 | + `material-icons-extended` |
| Architecture | ViewModel + StateFlow | lifecycle 2.10.0 | Hilt-injected ViewModels |
| DI | Hilt | 2.59.2 | + `hilt-navigation-compose` 1.2.0 |
| Local DB | Room | 2.7.1 | KSP compiler |
| Network | Retrofit + Gson converter | 2.11.0 | OkHttp 4.12.0 + BASIC logging |
| Images | Coil 3 | 3.1.0 | `coil-compose` + `coil-network-okhttp` |
| Navigation | Navigation Compose | 2.9.0 | |
| Playback | Android WebView | system | `androidyoutubeplayer:core` 12.1.0 is declared but the player is a custom WebView (see §16) |
| Async | kotlinx-coroutines-android | 1.10.1 | |
| Desugaring | desugar_jdk_libs | 2.1.5 | `java.time` on API < 26 |
| Min / target SDK | — | 23 / 36 | compileSdk 36 (minor 1) |

Leftover catalog entries not used by the app module: `google-signin` 21.3.0,
`security-crypto` 1.1.0-alpha06, `coroutines-play-services` (declared in
`libs.versions.toml` only; absent from `app/build.gradle.kts` dependencies except
`youtube-player`, which is depended on but not referenced by code).

---

## 3. Architecture

Clean-ish MVVM: **UI (Compose) → ViewModel → UseCase (feed/history/block only) → Repository
(interface in `domain`, impl in `data`) → Room / Retrofit**.

```
┌────────────┐   StateFlow    ┌─────────────┐  invoke()  ┌────────────┐
│ Composable │ ◄───────────── │  ViewModel  │ ─────────► │  UseCase   │
│  (Screen)  │ ── events ───► │  (Hilt)     │            │ (optional) │
└────────────┘                └─────────────┘            └─────┬──────┘
                                                               │
                                              ┌────────────────▼─────────────────┐
                                              │ Repository (domain interface)    │
                                              │  impl in data/repository          │
                                              └───────┬─────────────────┬────────┘
                                                      │                 │
                                            ┌─────────▼──────┐ ┌────────▼─────────┐
                                            │ Room (5 DAOs)  │ │ Retrofit         │
                                            │ mykidtube.db   │ │ YouTube Data v3  │
                                            └────────────────┘ └──────────────────┘
```

**Write path — example: parent adds a channel**
1. `SubscriptionsScreen` search row → `viewModel.addChannel(ch)`
2. `SubscriptionsViewModel.addChannel` → `CuratedChannelRepository.addChannel(channel)`
3. `CuratedChannelRepositoryImpl` → `CuratedChannelDao.insert(entity)` (REPLACE on conflict)
4. The `channels` StateFlow (backed by `dao.getAll()` Flow) re-emits → grid updates

**Read path — example: Home feed**
1. `HomeViewModel.init` collects `GetFeedUseCase()` =
   `FeedRepository.getFeed()` (ordered ids from `feed_videos` joined with `videos`)
   `combine` `BlockedChannelRepository.getBlockedChannels()` → blocked channels filtered out
2. Emission mapped to `UiState.Empty` / `UiState.Success`
3. `HomeScreen` renders grid; pull-to-refresh calls `RefreshFeedUseCase` (network)

**Error handling**
- Repositories return `Result<…>` (`runCatching`) for network calls; Flows are wrapped with
  `.catch { UiState.Error(GENERIC) }` in ViewModels.
- `SearchViewModel` maps exceptions: `HttpException 403 → QUOTA_EXCEEDED`,
  `UnknownHostException → NO_NETWORK`, else `GENERIC`.
- `HomeViewModel.refresh()` ignores the `Result` of refresh — stale feed simply stays.
- DB migrations: `fallbackToDestructiveMigration(dropAllTables = true)` — schema bump wipes data.

---

## 4. Package / Folder Structure

```
io.github.juliasivridi.kindtube
├── KindTubeApplication.kt        @HiltAndroidApp entry point
├── MainActivity.kt               single activity, edge-to-edge, MyKidTubeTheme + NavGraph
├── data
│   ├── local
│   │   ├── db                    AppDatabase (v2) + 5 DAOs
│   │   └── model                 5 Room entities + toDomain()/toEntity() mappers
│   ├── remote
│   │   ├── api                   YouTubeApiService (Retrofit interface, 5 endpoints)
│   │   ├── dto                   Gson DTOs: ChannelDto, PlaylistDto, VideoDto, SubscriptionDto*
│   │   ├── interceptor           ApiKeyInterceptor (adds ?key= to every request)
│   │   └── util                  DurationParser (ISO-8601 duration/timestamp)
│   └── repository                6 repository implementations
├── di                            DatabaseModule, NetworkModule, RepositoryModule
├── domain
│   ├── model                     Video, CuratedChannel, BlockedChannel, HistoryEntry, Channel*
│   ├── repository                6 repository interfaces (+ SearchResult)
│   └── usecase                   GetFeed, RefreshFeed, AddToHistory, BlockChannel
├── ui
│   ├── channel                   ChannelScreen + ChannelViewModel
│   ├── common                    VideoCard, ParentalControlDialog, EmptyView, ErrorView, LoadingView
│   ├── history                   HistoryScreen + HistoryViewModel
│   ├── home                      HomeScreen + HomeViewModel
│   ├── navigation                NavGraph (bottom bar + NavHost), Screen routes
│   ├── player                    PlayerScreen (custom WebView player) + PlayerViewModel
│   ├── search                    SearchScreen + SearchViewModel
│   ├── settings                  SettingsScreen + SettingsViewModel (blocked list, backup)
│   ├── subscriptions             SubscriptionsScreen + SubscriptionsViewModel
│   └── theme                     Color, Theme (MyKidTubeTheme), Type (Nunito), LocalTabColor
└── util                          UiState/ErrorType, Extensions (formatDuration, TTL consts)
```

`*` `SubscriptionDto` and the domain `Channel` model are unused remnants of the abandoned
Google-account prototype.

---

## 5. Data Model

### `Video`

| Field | Type | Description |
|---|---|---|
| `id` | `String` | YouTube video id (PK in cache) |
| `title` | `String` | |
| `thumbnailUrl` | `String` | `medium` thumbnail, falls back to `default` |
| `channelId` | `String` | |
| `channelTitle` | `String` | |
| `channelAvatarUrl` | `String?` | only populated for feed videos (taken from the curated channel); `null` for search/byId |
| `durationSeconds` | `Int?` | parsed from ISO-8601 `PT#H#M#S`; `null` in search results |
| `publishedAt` | `Long` | epoch ms; `0L` when timestamp unparseable |

### `CuratedChannel`

| Field | Type | Description |
|---|---|---|
| `id` | `String` | channel id (`UC…`) |
| `title` | `String` | |
| `avatarUrl` | `String?` | |
| `uploadsPlaylistId` | `String` | `UU…`; from `contentDetails.relatedPlaylists.uploads` |
| `addedAt` | `Long` | default `System.currentTimeMillis()` |

### `BlockedChannel`

| Field | Type | Description |
|---|---|---|
| `channelId` | `String` | |
| `channelTitle` | `String` | |
| `channelAvatarUrl` | `String?` | |
| `blockedAt` | `Long` | epoch ms |

### `HistoryEntry`

| Field | Type | Description |
|---|---|---|
| `id` | `Long` | autoincrement, default 0 |
| `video` | `Video` | denormalized — full video snapshot stored in the row |
| `watchedAt` | `Long` | epoch ms |

**Invariant:** `HistoryRepositoryImpl.addToHistory` deletes any existing row with the same
`videoId` before inserting — re-watching moves the entry to the top instead of duplicating.

**Invariant (blocking):** `BlockChannelUseCase` both inserts into `blocked_channels` *and*
removes the channel from `curated_channels`. Blocked channels are additionally filtered:
in the feed (`GetFeedUseCase.combine`), in video search results and in channel search results.

### `UiState<T>` / `ErrorType` (util)

```
UiState: Loading | Empty | Success(data: T) | Error(type: ErrorType)
ErrorType: NO_NETWORK | QUOTA_EXCEEDED | AUTH_EXPIRED | GENERIC
```
`AUTH_EXPIRED` is declared but never produced (sign-in was removed).

### Backup JSON (v1.1, `SettingsViewModel`)

```json
{ "app": "KindTube", "version": 1,
  "channels": [ { "id": "UC…", "title": "…", "avatarUrl": "…", "uploadsPlaylistId": "UU…" } ] }
```
On import every field except `id`+`title` is optional; a missing `uploadsPlaylistId` is
derived as `"UU" + id.removePrefix("UC")`. Import is an upsert (REPLACE by id); parse
failure returns `-1` → "Import failed" toast.

---

## 6. Database Schema (Room)

**File:** `mykidtube.db` · **version 2** · `exportSchema = false` ·
`fallbackToDestructiveMigration(dropAllTables = true)` — no migration history is kept;
a version bump drops all tables.

| Table | Columns (PK bold) | Notes |
|---|---|---|
| `videos` | **id** TEXT, title, thumbnailUrl, channelId, channelTitle, channelAvatarUrl?, durationSeconds INT?, publishedAt INT, cachedAt INT | cache for feed/channel/player; `cachedAt` default now |
| `curated_channels` | **id** TEXT, title, avatarUrl?, uploadsPlaylistId, addedAt INT | the whitelist |
| `blocked_channels` | **channelId** TEXT, channelTitle, channelAvatarUrl?, blockedAt INT | |
| `history` | **id** INT autoincrement, videoId, videoTitle, videoThumbnailUrl, videoChannelId, videoChannelTitle, videoChannelAvatarUrl?, videoDurationSeconds INT?, videoPublishedAt INT, watchedAt INT | denormalized Video snapshot |
| `feed_videos` | **videoId** TEXT, position INT | precomputed feed order; cleared + rebuilt on every refresh |

DAO highlights: all list queries return `Flow`; inserts use `OnConflictStrategy.REPLACE`;
`VideoDao.deleteVideosByChannel` clears a channel's cache before re-insert;
`BlockedChannelDao.isBlocked` is an `EXISTS` query.

---

## 7. First-Launch Setup

There is no auth and no onboarding. On first launch:

1. Room creates `mykidtube.db` (empty).
2. Home feed: `curated_channels` is empty → `refreshFeed()` clears `feed_videos` and returns →
   `UiState.Empty` → message *"No videos.\nAdd channels in the Subscriptions tab!"*.
3. The parent adds channels via Subscriptions search (or Settings → Import JSON since v1.1).
4. The YouTube API key is compiled in via `BuildConfig.YOUTUBE_API_KEY` (from
   `local.properties` key `youtubeApiKey`, or the `YOUTUBE_API_KEY` env var in CI).

The activity is locked to `sensorLandscape`; the only Android permission is `INTERNET`.

---

## 8. YouTube Data API Layer

Base URL `https://www.googleapis.com/youtube/v3/`; every request gets `?key=` appended by
`ApiKeyInterceptor`. No retry/backoff logic — failures surface as `Result.failure`.

| Method | Endpoint | part | Notes |
|---|---|---|---|
| `getChannels(ids)` | `channels` | `snippet,contentDetails` | resolves uploads playlist id |
| `getPlaylistItems(playlistId, maxResults, pageToken?)` | `playlistItems` | `snippet,contentDetails` | default maxResults 20 |
| `getVideos(ids)` | `videos` | `snippet,contentDetails,status` | `status.embeddable` used for filtering |
| `searchVideos(q, pageToken?)` | `search` | `snippet` | `type=video`, `safeSearch=strict`, `videoEmbeddable=true`, maxResults 25 |
| `searchChannels(q)` | `search` | `snippet` | `type=channel`, maxResults 10 |

Per-repository behavior:

- **FeedRepositoryImpl.refreshFeed()** — see §16 (feed algorithm). Per channel: playlistItems
  (maxResults 50) → `getVideos` → drop `status.embeddable == false` → cache all in `videos`,
  pick 3–5 random for the feed. Failures of a single channel are swallowed (`runCatching`)
  — the feed is built from whatever succeeded.
- **VideoRepositoryImpl.refreshChannelVideos()** — playlistItems (maxResults 20) → `getVideos`
  → embeddable filter → `deleteVideosByChannel` + insert (cache replace).
  `getVideoById` reads the Room cache first; network fallback `getVideos(ids = videoId)`.
- **SearchRepositoryImpl.searchVideos()** — `search` (embeddable enforced server-side) →
  `getVideos` for full snippets → drops videos from blocked channels. Returns
  `SearchResult(videos, nextPageToken)` for pagination.
- **CuratedChannelRepositoryImpl.searchChannels()** — `search(type=channel)` →
  `getChannels` for uploads ids → drops blocked channels.

Offline behavior: feed, channel videos and history render from Room; refresh quietly fails.
Search and the player require the network.

---

## 9. UI Screens

The app is a single `Scaffold` (in `NavGraph`) whose `NavigationBar` is shown only on the
4 tab routes. The bar has centered icon buttons (50dp spacing) and is tinted with the active
tab's pastel `barColor`; the same color is exposed to screens via `LocalTabColor` and used
as the `TopAppBar` container color. Main background: vertical gradient `GradientTop→GradientBottom`.

| Tab | Icon | iconColor | barColor |
|---|---|---|---|
| Home | `Icons.Filled.Home` | `0xFF1565C0` | `0xFFBBDEFB` |
| History | `Icons.Filled.History` | `0xFFE65100` | `0xFFFFE0B2` |
| Subscriptions | `Icons.Filled.Subscriptions` | `0xFF2E7D32` | `0xFFC8E6C9` |
| Search | `Icons.Filled.Search` | `0xFFC62828` | `0xFFFFCDD2` |

### HomeScreen

- **Route** `home` · **ViewModel** `HomeViewModel`
- **Data:** `uiState: StateFlow<UiState<List<Video>>>` from `GetFeedUseCase`
  (feed minus blocked channels); `isRefreshing: StateFlow<Boolean>`.
  `init` both observes the feed and triggers `refresh()`.
- **Layout:** TopAppBar (title "KindTube", actions: Search icon → switch to Search tab;
  Menu icon ☰ → `ParentalControlDialog` → Settings). Body: `PullToRefreshBox` wrapping a
  `LazyVerticalGrid(Adaptive(200.dp))`, contentPadding 50/20dp, spacing 20dp, `VideoCard` items.
- **Empty:** *"No videos.\nAdd channels in the Subscriptions tab!"*
- **Actions:** card tap → `player/{videoId}`; pull-to-refresh → `RefreshFeedUseCase`
  (new random feed every time).

### HistoryScreen

- **Route** `history` · **ViewModel** `HistoryViewModel`
- **Data:** `uiState` from `HistoryRepository.getHistory()` (DAO orders by `watchedAt DESC`).
- **Layout:** TopAppBar "History" with `DeleteSweep` action → confirm dialog
  ("Clear history?" / "All watched videos will be removed from history." / Clear / Cancel)
  → `clearHistory()`. Same adaptive grid of `VideoCard`s.
- **Empty:** *"Your watch history is empty"*.

### SubscriptionsScreen

- **Route** `subscriptions` · **ViewModel** `SubscriptionsViewModel`
- **Data:** `channels` (Room Flow, `stateIn` WhileSubscribed 5000); UI sorts by
  `title.lowercase()`. Search: `searchQuery`, `searchResults`, `isSearching`,
  `searchError` (unused in UI). Query is debounced **600 ms**; results cleared on each change.
- **Layout:** `LazyVerticalGrid(Fixed(4))`, 12dp paddings/spacing.
  Full-span search field (placeholder *"Add a channel — Peppa Pig, Paw Patrol..."*,
  trailing 20dp `CircularProgressIndicator` while searching) → full-span result rows
  (40dp round avatar + title + `Add` icon button) → header *"My channels (N)"* →
  `ChannelGridItem` cells: full-width 1:1 round avatar (clickable → Channel screen),
  name + ⋮ (28dp `IconButton`, 16dp `MoreVert`) with dropdown *"Remove from subscriptions"*
  (red `Delete` icon) gated by `ParentalControlDialog`.
- **Empty list text:** *"Add channels using the search above"*.

### SearchScreen

- **Route** `search` · **ViewModel** `SearchViewModel`
- **Data:** `query` + `uiState`; initial state `Empty`. Debounce **500 ms**; new query →
  `UiState.Loading`; pagination appends pages (`existing + result.videos`).
- **Infinite scroll:** `snapshotFlow { gridState.layoutInfo }` → when last visible index ≥
  `totalItemsCount - 4` → `loadMore()` (uses `lastResult.nextPageToken`).
- **Layout:** TopAppBar contains an `OutlinedTextField` (placeholder *"Search videos..."*,
  leading Search icon, trailing Clear when non-empty, IME Search hides keyboard).
  Body: adaptive grid of `VideoCard`s.
- **Empty:** icon `SearchOff`; *"Enter a search query"* when blank, otherwise
  *"Nothing found for\n"<query>""*.
- **Errors:** 403 → QUOTA_EXCEEDED, UnknownHost → NO_NETWORK, else GENERIC.

### PlayerScreen

- **Route** `player/{videoId}` · **ViewModel** `PlayerViewModel`
- **Data:** `video: StateFlow<Video?>` (Room/byId; side effect: `AddToHistoryUseCase`),
  `relatedVideos: StateFlow<List<Video>>` — 2–4 random videos of the same channel
  (excluding current) + shuffled feed videos from other channels, `take(20)`, no extra API calls.
- **Player internals:** see §16 (Chrome-less WebView player). Local state: `overlayVisible`
  (auto-hide 3000 ms via `overlayResetKey`; pinned while paused/ended), `panelVisible`,
  `progress`/`durationSec` (polled every 500 ms), `playerState` (-1/0/1/2), `isPaused`.
- **Gestures:** tap anywhere → toggles *everything* (controls + wide bar + panel);
  swipe down (> 30f) → closes the panel; all touches are intercepted before the WebView.
- **Top overlay** (88dp, black→transparent gradient, fade in/out): back arrow (`ArrowBack`),
  clickable title column (channelTitle `labelSmall` white@70% over title `bodyMedium`
  SemiBold white; tap → Channel screen via `UC→UU` uploads-id derivation in NavGraph),
  `Block` icon → `ParentalControlDialog` → `blockChannel()` + navigate Home.
- **Bottom controls row** (black@60%): 64dp play/pause/replay IconButton (48dp icon —
  `Replay` when ended, `PlayArrow` when paused, else `Pause`), `VideoSeekBar` (weight 1),
  time text `mm:ss / mm:ss` (96dp width). When the overlay is hidden a 3dp red
  `LinearProgressIndicator` (track white@25%) is shown instead.
- **"More videos" panel** (black@90%, slides from bottom, below the progress bar):
  header row + `LazyRow` of `CompactVideoCard`s; closed by arrow button, swipe-down,
  or picking a video (which replaces the Player destination via `popUpTo(player) inclusive`).
- **End of video** (`playerState == 0`): full cover — thumbnail + black@45% scrim +
  central 88dp circular Replay button (56dp icon, black@55% circle); overlay & panel pinned.
- **Lifecycle:** ON_PAUSE/ON_RESUME → `webView.onPause()/onResume()`; dispose → `destroy()`.

### ChannelScreen

- **Route** `channel/{channelId}/{uploadsPlaylistId}` · **ViewModel** `ChannelViewModel`
  (args via `SavedStateHandle`)
- **Data:** Room Flow `getChannelVideos(channelId)` (ordered `publishedAt DESC`);
  `refresh()` on init and via `PullToRefreshBox`.
- **Layout:** TopAppBar "Channel videos" + back arrow; adaptive grid of `VideoCard`s.
- **Empty:** *"No videos for this channel"*.

### SettingsScreen

- **Route** `settings` (entry itself is behind the parental gate on Home)
  · **ViewModel** `SettingsViewModel`
- **Data:** `blockedChannels` (Room Flow, WhileSubscribed 5000).
- **Layout:** TopAppBar "Settings" + back. `LazyVerticalGrid(Fixed(4))`:
  header *"Blocked channels (N)"* → `BlockedChannelGridItem` cells (avatar NOT clickable;
  ⋮ menu → *"Unblock"* with `LockOpen` icon, gated by `ParentalControlDialog`) →
  full-span column: **Force refresh feed** button (`clearCache()` = `refreshFeed()`),
  divider, **Channel list backup** section (v1.1): *Export JSON* (SAF `CreateDocument`
  with filename `kindtube-channels-YYYY-MM-DD.json`) and *Import JSON* (SAF `OpenDocument`,
  MIME `application/json`); results reported with Toasts ("Channels exported" /
  "Imported N channels" / "Import failed: not a KindTube backup"), divider,
  *"Version 1.1.0"* caption.
- **Empty blocked list text:** *"No blocked channels"*.

---

## 10. Key Components

### `VideoCard(video: Video, onClick: () -> Unit, modifier: Modifier = Modifier)`

Card (12dp corners, 2dp elevation): 16:9 `AsyncImage` (top corners clipped 12dp) with a
duration badge bottom-right (`formatDuration`, `labelSmall` white on black@72%, 4dp corners,
outer padding 6dp, inner 5×2dp) shown only when `durationSeconds != null`; below — title,
`bodyMedium` SemiBold, maxLines 2, padding 10×8dp. No channel row, no avatar (kid UI).

### `ParentalControlDialog(onSuccess: () -> Unit, onDismiss: () -> Unit)`

`AlertDialog` titled **"For parents"**. Internal state: `a`, `b` (random 2..9 each),
`answer: String`, `isError`. Question text *"What is  a × b ?"* (`headlineSmall`).
Number-only `OutlinedTextField` (centered 32sp), IME Done verifies. Wrong answer → field
turns red with supporting text *"Wrong answer, try again"*, a **new** problem is generated,
input cleared; unlimited attempts. Buttons: **Confirm** / **Cancel**.

### `EmptyView(message: String, modifier, icon: ImageVector = Icons.Filled.TvOff)`

Centered column: 72dp icon tinted `primary@50%`, 16dp spacer, message `bodyLarge`
centered `onSurface@60%`.

### `ErrorView(errorType: ErrorType, onRetry: (() -> Unit)? = null, modifier)`

Centered column (32dp padding): 72dp icon in `error` color + message
(`CloudOff` *"No internet.\nCheck your connection!"* · `Lock` *"YouTube quota exceeded.\nTry again tomorrow"*
· `Lock` *"Login required"* · `ErrorOutline` *"Something went wrong"*); optional **Retry** button (24dp above).

### `LoadingView(modifier)` — centered `CircularProgressIndicator`.

### `VideoSeekBar(progress: Float, onSeek: (Float) -> Unit, modifier)` *(private to PlayerScreen)*

Canvas, 40dp tall. Track: rounded rect 16dp high, white@30%; filled portion red; thumb:
white circle r=13dp. Tap-to-seek and drag-to-seek via `awaitEachGesture` +
`awaitFirstDown(requireUnconsumed=false)`; events consumed so they don't reach the parent
toggle. `onSeek` is called continuously during drag with the fraction `x/width` (0..1).

### `CompactVideoCard(video, onClick)` *(private to PlayerScreen)*

180dp wide column: 16:9 thumbnail (8dp corners), 4dp spacer, title `labelMedium` white,
maxLines 2.

### `ChannelGridItem` / `BlockedChannelGridItem` *(private)*

Grid cell: full-width 1:1 circular `AsyncImage` (surfaceVariant placeholder bg), then a
name row (`labelMedium`, ellipsis) with a 28dp ⋮ button (16dp icon, onSurface@55%) opening
a one-item `DropdownMenu` (remove / unblock). Curated avatar is clickable → channel; blocked
avatar is not clickable.

---

## 11. Theme & Colors

Material 3, light/dark by `isSystemInDarkTheme()` (theme function name is still
`MyKidTubeTheme`). Typography is **Nunito** (5 weights bundled in `res/font`).
Main background uses a static mint gradient instead of `background` color.

| Constant | Light | Dark | Usage |
|---|---|---|---|
| Primary | `0xFF5B9BD5` | `0xFF93C4E8` | accents, section headers |
| OnPrimary | `0xFFFFFFFF` | `0xFF0D2D45` | |
| PrimaryContainer | `0xFFD6E8F7` | `0xFF1E4A6E` | |
| OnPrimaryContainer | `0xFF1A3D5C` | `0xFFD6E8F7` | |
| Secondary | `0xFFF4A261` | `0xFFF4BC8A` | |
| OnSecondary | `0xFFFFFFFF` | `0xFF4A2800` | |
| Tertiary | `0xFF6CBF85` | `0xFF98D4A8` | |
| Error | `0xFFE07B6A` | `0xFFEFA898` | ErrorView icon, destructive menu items |
| Background | `0xFFD8F0E6` | `0xFF1A1C2A` | |
| Surface | `0xFFFFFFFF` | `0xFF242636` | cards |
| OnSurface | `0xFF1C1B1F` | `0xFFE4E2E6` | |
| SurfaceVariant | `0xFFEEF3F9` | `0xFF2E3245` | avatar placeholders |
| GradientTop / GradientBottom | `0xFFD4EFE7` / `0xFFB3E5D3` | (same) | app background gradient |

`LocalTabColor: CompositionLocal<Color>` (default `Transparent`) carries the active tab's
pastel bar color into each screen's `TopAppBar`.

---

## 12. Navigation

Single `NavHost`, start destination `home`. No deeplinks.

| Route | Screen | Args | Bottom bar |
|---|---|---|---|
| `home` | HomeScreen | — | yes |
| `history` | HistoryScreen | — | yes |
| `subscriptions` | SubscriptionsScreen | — | yes |
| `search` | SearchScreen | — | yes |
| `player/{videoId}` | PlayerScreen | videoId: String | no |
| `channel/{channelId}/{uploadsPlaylistId}` | ChannelScreen | both String | no |
| `settings` | SettingsScreen | — | no |

Notable transitions: tab switches use `popUpTo(startDestination) { saveState=true }` +
`launchSingleTop` + `restoreState`; picking a related video replaces the player
(`popUpTo(player) { inclusive=true }`); blocking a channel from the player navigates Home;
the Player's title tap derives `uploadsPlaylistId = "UU" + channelId.removePrefix("UC")`
(only when the id starts with `UC`).

---

## 13. Loading & Empty States

No shimmer/skeletons — loading is a centered `CircularProgressIndicator` (`LoadingView`).

| Screen | Empty icon | Empty message |
|---|---|---|
| Home | `TvOff` (default) | "No videos.\nAdd channels in the Subscriptions tab!" |
| History | `TvOff` | "Your watch history is empty" |
| Search | `SearchOff` | "Enter a search query" / "Nothing found for\n"<query>"" |
| Channel | `TvOff` | "No videos for this channel" |
| Subscriptions (list) | — (plain text) | "Add channels using the search above" |
| Settings (blocked) | — (plain text) | "No blocked channels" |

In the Player the thumbnail (`Layer 1`) doubles as the loading placeholder while the
embed page loads.

---

## 14. CI/CD & Build

`.github/workflows/release.yml` — trigger: push of a `v*` tag.

1. `actions/checkout@v5`, `actions/setup-java@v5` (Temurin 17), `chmod +x gradlew`
2. Decode `secrets.KEYSTORE_BASE64` → `keystore.jks`
3. `./gradlew assembleRelease bundleRelease` with env: `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`,
   `KEY_ALIAS`, `KEY_PASSWORD`, `YOUTUBE_API_KEY`
4. Rename to `kindtube.apk` / `kindtube.aab`
5. `softprops/action-gh-release@v2` attaches both to the GitHub Release

Signing: env-driven `signingConfigs.release` in `app/build.gradle.kts` (no keystore in repo).
The API key resolution order: `local.properties[youtubeApiKey]` → `YOUTUBE_API_KEY` env → `""`.
Release build: `isMinifyEnabled = false`.

---

## 15. First-Time Setup (New Developer)

1. Install Android Studio (AGP 9.1 requires a current Narwhal+ version) with JDK 17+.
2. `git clone git@github.com:JuliaSivridi/KindTube.git`
3. Google Cloud Console → create/select a project → enable **YouTube Data API v3** →
   Credentials → **API key**.
4. Add to `local.properties` (never committed): `youtubeApiKey=AIza…`
5. Run ▶ on a device/emulator (landscape tablet recommended; min API 23).
6. For CI releases add repo secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`,
   `KEY_PASSWORD`, `YOUTUBE_API_KEY`, then:
   ```bash
   git tag v1.2.0 && git push origin v1.2.0
   ```

---

## 16. Key Algorithms

### Chrome-less WebView player (the core trick)

```
load("https://www.youtube-nocookie.com/embed/{id}
      ?autoplay=1&controls=0&rel=0&modestbranding=1
      &iv_load_policy=3&playsinline=1&fs=0&disablekb=1",
     headers = { Referer: "https://www.youtube-nocookie.com/" })   // no Referer → error 153

onPageFinished → inject PLAYER_SETUP_JS:
    style: body * { visibility: hidden !important }
           video  { visibility: visible !important }   // hides ALL chrome, class-name-proof
    _play()/_pause()/_seek(s)  → direct <video> control
    _poll() → [currentTime/duration, duration, state]  // state: 0 ended, 1 playing, 2 paused

every 500 ms: evaluateJavascript("_poll()") → progress, duration, playerState
              playerState 1→isPaused=false, 2→isPaused=true   // UI never desyncs

navigation: shouldOverrideUrlLoading → true (block everything)
            onCreateWindow → false (no popups)
            full-size Compose Box intercepts ALL touches above the WebView
```

Why main-frame + CSS instead of alternatives (all tried and rejected):
iframe + raw postMessage — commands silently ignored without the widget handshake;
`YT.Player` JS API — `controls=0` does not suppress the mobile embed's tap/pause overlays;
hiding `.ytp-*` classes — the mobile player uses different class names.

**Compose gotcha:** a consume-everything `pointerInput` on a container cancels its
children's `clickable`s. Containers that must swallow background taps (controls row, panel)
use a no-op `clickable(indication = null)` instead.

### Feed building (`FeedRepositoryImpl.refreshFeed`)

```
channels ← curated_channels; if empty: clear feed_videos, return
sampled  ← channels.shuffled().take(12)
feed     ← []
for ch in sampled:                       # each channel isolated in runCatching
    ids    ← playlistItems(ch.uploadsPlaylistId, max 50).videoIds
    videos ← getVideos(ids) minus { status.embeddable == false }
    feed  += videos.shuffled().take(random(3..5))
    cache all videos in `videos` table
feed_videos ← feed.shuffled() with positions 0..n   # clear + insert
```
Reads (`getFeed`) join `feed_videos.position` order with the `videos` cache, then
`GetFeedUseCase` filters blocked channels reactively.

### Related videos (`PlayerViewModel`)

```
relatedVideos = video.filterNotNull().flatMapLatest {
    combine(channelVideos(it.channelId), feed) { own, feedVideos ->
        own.filter(id != current).shuffled().take(4)
        + feedVideos.filter(id != current && channelId != current.channel).shuffled()
    }.take(20)
}
```

### Parental gate

```
a, b ← random(2..9) each
correct ⇔ answer.trim().toIntOrNull() == a*b
wrong → isError, regenerate a and b, clear input (unlimited attempts)
```

### ISO-8601 parsing (`DurationParser`)

```
PT(\d+H)?(\d+M)?(\d+S)? → h*3600 + m*60 + s   (null on blank/no match)
Instant.parse(timestamp).toEpochMilli()        (0L on failure)
```

### Uploads-playlist derivation

YouTube convention used in two places (NavGraph title-tap, backup import):
`uploadsPlaylistId = "UU" + channelId.removePrefix("UC")` — applied only to `UC…` ids.
