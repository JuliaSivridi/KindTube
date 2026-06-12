# MyKidTube — Техническая архитектура

> Версия 3 — обновлена 2026-04-09. Отражает переход с OAuth → YouTube Data API v3 (API key).

## 1. Технологический стек

| Слой | Технология | Обоснование |
|---|---|---|
| Язык | Kotlin | Нативный Android |
| Асинхронность | Coroutines + Flow | Стандарт для Kotlin |
| DI | Hilt 2.59.2 | Google-рекомендуемый, минимум boilerplate |
| Сеть | Retrofit 2 + OkHttp 3 | Стандарт, хорошая интеграция с Coroutines |
| Локальная БД | Room 2.7 | Jetpack, поддержка Flow, персистентное кэширование |
| Изображения | Coil 3 | Kotlin-first, лёгкий |
| **Авторизация** | **Нет (API key)** | **Google Family Link запрещает мульти-аккаунт на устройстве** |
| Плеер | WebView + youtube-nocookie.com | YouTubePlayerView даёт ошибку 152 на ограниченных видео |
| Навигация | Navigation Compose + Bottom Navigation | Jetpack-стандарт |
| Тема | Material Design 3 (светлая/тёмная) | Следует системным настройкам |

**Целевое устройство:** Samsung Galaxy Tab (Android 7.0 API 24, landscape).
**minSdk:** 23 (с `isCoreLibraryDesugaringEnabled = true` для `java.time.*`)

---

## 2. Причина отказа от OAuth

Google Family Link управляет детским аккаунтом на планшете. На одном устройстве **нельзя** одновременно иметь родительский и детский аккаунты Google залогиненными. Поэтому:

- OAuth-авторизация (Google Sign-In) невозможна
- Вместо подписок из YouTube используется **список кураторских каналов** (родитель добавляет вручную через поиск в приложении)
- YouTube Data API v3 работает с API-ключом без авторизации
- `safeSearch=strict` работает без OAuth (серверный параметр YouTube)

**API-ключ** хранится в `local.properties` (не в git), читается через `BuildConfig` в `build.gradle.kts`.

---

## 3. Модульная структура проекта

```
app/
├── data/
│   ├── local/
│   │   ├── db/           # Room database, DAOs
│   │   └── model/        # Room Entity классы
│   ├── remote/
│   │   ├── api/          # Retrofit интерфейс YouTubeApiService
│   │   ├── dto/          # DTO классы (JSON → Kotlin)
│   │   └── interceptor/  # ApiKeyInterceptor (добавляет ?key=...)
│   └── repository/       # Реализации репозиториев
├── domain/
│   ├── model/            # Доменные модели (чистые data class)
│   ├── repository/       # Интерфейсы репозиториев
│   └── usecase/          # Use cases (бизнес-логика)
├── ui/
│   ├── home/             # Лента (главная)
│   ├── history/          # История
│   ├── subscriptions/    # Управление каналами (поиск + список)
│   ├── search/           # Поиск видео
│   ├── player/           # Плеер (WebView)
│   ├── channel/          # Экран канала
│   ├── settings/         # Настройки (скрытые, родительский контроль)
│   └── common/           # Общие компоненты (VideoCard, ErrorView, ParentalControlDialog...)
├── di/                   # Hilt модули
└── util/                 # Вспомогательные функции
```

---

## 4. Архитектурные слои (MVVM + Repository)

```
UI (Composable)
    ↕ StateFlow
ViewModel
    ↕ suspend fun / Flow
UseCase (бизнес-логика)
    ↕
Repository (интерфейс)
    ↕
┌────────────────────┬──────────────────────┐
RemoteDataSource      LocalDataSource
(Retrofit API)        (Room)
```

---

## 5. База данных (Room v2)

| Таблица | Назначение |
|---|---|
| `videos` | Все загруженные видео (для плеера + related) |
| `feed_videos` | Текущая лента: (videoId, position) — обновляется при refresh |
| `curated_channels` | Каналы, добавленные родителем |
| `history` | История просмотров |
| `blocked_channels` | Заблокированные каналы |

`fallbackToDestructiveMigration()` — в dev-режиме при смене схемы.

---

## 6. Стратегия формирования ленты

### Алгоритм (реализован в `FeedRepositoryImpl.refreshFeed()`)

1. Получить все `curated_channels` из Room
2. Случайно выбрать **min(12, N)** каналов
3. Для каждого из выбранных:
   - `playlistItems.list` → IDs последних 50 видео (1 ед. квоты)
   - `videos.list` → метаданные (1 ед. квоты)
   - Случайно выбрать **3–5 видео** из результатов
4. Вставить все видео в таблицу `videos` (upsert)
5. Перемешать выборку → записать порядок в `feed_videos`

### Pull-to-refresh
Каждый pull-to-refresh = новая случайная выборка каналов → новая лента.
Гарантирует ротацию: "активные" каналы не доминируют.

### Константы
```kotlin
const val CHANNELS_TO_SAMPLE = 12
const val VIDEOS_MIN_PER_CHANNEL = 3
const val VIDEOS_MAX_PER_CHANNEL = 5
```

### Расход квоты (10 каналов в базе)
| Действие | Единиц |
|---|---|
| playlistItems.list × 10 каналов | 10 |
| videos.list × 10 запросов | 10 |
| Поиск видео (2 раза/день) | 200 |
| Поиск каналов (добавление) | 100 |
| **Дневной итог** | **~320 из 10 000** |

---

## 7. YouTube Player

Используется `WebView`. Страница грузится через `loadDataWithBaseURL("https://www.youtube-nocookie.com", html, ...)`.
HTML содержит `<iframe src="embedUrl">`. Это единственный рабочий способ — прямой `loadUrl(embedUrl)` и `YT.Player` JS API не работают на ограниченных устройствах (ошибка 152) или конфликтуют с навигационными блокировками.

### Embed URL
```
https://www.youtube-nocookie.com/embed/{videoId}
  ?autoplay=1
  &controls=0          ← убирает ВСЕ нативные элементы YouTube (прогресс, субтитры, настройки)
  &rel=0
  &modestbranding=1
  &iv_load_policy=3
  &playsinline=1
  &enablejsapi=1       ← нужен для infoDelivery postMessage
  &origin=https://www.youtube-nocookie.com
```

### Блокировка навигации
`WebViewClient.shouldOverrideUrlLoading` возвращает `true` для **всех** URL.
Это блокирует переходы по ссылкам на каналы, видео и внешние сайты внутри WebView.
`WebChromeClient.onCreateWindow` возвращает `false` — блокирует всплывающие окна.

### Слои (Box)
```
Layer 1 — AsyncImage (thumbnail)  ← виден пока iframe не загрузился
Layer 2 — AndroidView (WebView)   ← прозрачный фон, загружает iframe
Layer 3 — Compose Box (overlay)   ← перехватывает все касания
             ├── Верхний оверлей (кнопка ←, название видео, кнопка блокировки)
             ├── Кнопка "Ещё видео ↑"
             ├── Полоска прогресса (LinearProgressIndicator, красная, 3dp)
             └── Панель похожих видео (свайп вверх)
```
Layer 3 стоит последним в Box → перехватывает касания раньше WebView.

### Кастомный прогресс-бар
`controls=0` убирает нативный прогресс YouTube. Свой прогресс-бар реализован так:
1. HTML-страница слушает `window.addEventListener('message')` — принимает `infoDelivery` события от iframe
2. Сохраняет `window._c` (currentTime) и `window._d` (duration)
3. Kotlin каждые 500 мс вызывает `webView.evaluateJavascript(...)` — читает эти переменные
4. Также пробует прямой доступ к `<video>` элементу через `iframe.contentDocument` (same-origin)
5. Обновляет `MutableFloatState` → `LinearProgressIndicator` перерисовывается
Никакого `@JavascriptInterface` не нужно — `evaluateJavascript` вызывается с main thread и возвращает результат через callback.

---

## 8. Навигация

```
MainActivity
└── NavGraph (Compose Navigation)
    ├── Scaffold (containerColor=Transparent, фоновый градиент в Box внутри)
    │   ├── NavigationBar (видна только на tab-экранах, фон = цвет активной вкладки)
    │   │   └── 4 IconButton по центру (Arrangement: Spacer weight(1f) + 50dp между иконками)
    │   └── NavHost
    │       ├── HomeScreen          (tab: Главная,  containerColor=Transparent)
    │       ├── HistoryScreen       (tab: История,  containerColor=Transparent)
    │       ├── SubscriptionsScreen (tab: Подписки, containerColor=Transparent)
    │       ├── SearchScreen        (tab: Поиск,    containerColor=Transparent)
    │       ├── PlayerScreen        (fullscreen WebView, нет bottom nav)
    │       ├── ChannelScreen       (back stack, нет bottom nav)
    │       └── SettingsScreen      (доступ через парентал-контроль)
```

**Переход Player → Player (похожее видео):** `popUpTo(Screen.Player.route) { inclusive = true }` — кнопка "Назад" возвращает на экран до плеера, не на предыдущее видео.

**Доступ в Настройки:** `HomeScreen → [☰] → ParentalControlDialog → SettingsScreen`

---

## 9. Родительский контроль

```kotlin
// ParentalControlDialog.kt
// Числа 2–9 (нет умножений на 0 или 1)
var a = (2..9).random()
var b = (2..9).random()
// Неправильный ответ → поле краснеет, новый пример
// Правильный ответ → onSuccess()
```

**Защищённые действия:**
- Вход в Настройки (`HomeScreen → [☰]`)
- Блокировка канала (`PlayerScreen → [🚫]`)
- Удаление канала из подписок (`SubscriptionsScreen → [🗑]`)

---

## 10. Тема и цвета

- **Режим:** светлый по умолчанию (тёмный по системным настройкам)
- **Шрифт:** Nunito — файлы `res/font/nunito_*.ttf`, подключён через `Type.kt`
- **Фоновый градиент** (NavGraph → Box, все Scaffold прозрачны):
  - Top: `#D4EFE7` (очень светлая мята)
  - Bottom: `#B3E5D3` (мягкая мята)
- **Цвета вкладок** (иконка + панели верха/низа):
  | Вкладка | Иконка | Панели (фон) |
  |---|---|---|
  | Главная | `#1565C0` синий | `#BBDEFB` светло-голубой |
  | История | `#E65100` оранжевый | `#FFE0B2` персиковый |
  | Подписки | `#2E7D32` зелёный | `#C8E6C9` светло-зелёный |
  | Поиск | `#C62828` красный | `#FFCDD2` светло-розовый |
- **LocalTabColor** — CompositionLocal, передаёт цвет панели из NavGraph в TopAppBar каждого экрана
- **Иконка приложения:** белый треугольник (play) на синем фоне `#3A7DC9`

---

## 11. Управление API-квотой

- При ошибке `403 quotaExceeded` → ErrorView с типом `QUOTA_EXCEEDED`
- Показывается кэшированная лента (из `feed_videos` + `videos`)
- Поиск видео: debounce 800 мс

---

## 12. Обработка ошибок

| Ситуация | Поведение |
|---|---|
| Нет интернета, есть кэш | Показать кэш (feed_videos из Room) |
| Нет интернета, нет кэша | ErrorView `NO_NETWORK` + кнопка "Повторить" |
| Квота API исчерпана | ErrorView `QUOTA_EXCEEDED` |
| Прочие ошибки | ErrorView `GENERIC` + кнопка "Повторить" |
