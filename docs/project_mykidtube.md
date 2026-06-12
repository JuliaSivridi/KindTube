---
name: MyKidTube project context
description: Детское Android-приложение MyKidTube — ключевые решения и контекст проекта
type: project
---

Нативное Android-приложение (Kotlin + Jetpack Compose) для 4-летнего ребёнка на планшете Samsung SM-T819. Безопасный просмотр YouTube без рекомендательного алгоритма — только каналы из подписок аккаунта ребёнка.

**Название:** MyKidTube (переименовано с YTube — Google запрещает "YouTube" в названиях приложений)
**Тип проекта:** личный, публикация в Play Store не планируется минимум год.
**Package name:** com.mykidtube.app

## Структура проекта

```
D:\Projects\MyKidTube\          ← корень (= Android-проект)
├── app\                        ← Android app module
├── docs\                       ← вся документация проекта
│   ├── YTube_prd.md            ← ТЗ (содержимое обновлено на MyKidTube)
│   ├── architecture.md
│   ├── screens_ux.md
│   ├── data_models.md
│   └── open_questions.md
├── .claude\                    ← Claude Code project config
└── gradle\
    └── libs.versions.toml      ← все зависимости
```

## Технический стек (подтверждён, проект собирается)

- AGP: 9.1.0, Kotlin: 2.2.10 (встроен в AGP — отдельный kotlin.android плагин НЕ нужен)
- UI: Jetpack Compose (сгенерировано AS — не XML/Fragments)
- DI: Hilt 2.59.2 (2.59+ обязателен для AGP 9.x)
- Annotation processing: KSP 2.3.6 (с 2.3.0 независим от Kotlin-версии)
- Room 2.7.1, Retrofit 2.11.0 + OkHttp 4.12.0
- Coil 3.1.0, Navigation Compose 2.9.0
- YouTubePlayerView 12.1.0 (pierfrancescosoffritti)
- Google Sign-In 21.3.0 (алиас: google-signin, НЕ google-sign-in — "in" = keyword Kotlin)
- Security Crypto 1.1.0-alpha06, Coroutines 1.10.1

## Ключевые решения

- Авторизация: аккаунт ребёнка supervised через Family Link
  → добавлен как Test User в OAuth consent screen (Google Cloud Console)
  → шаги 1-3 выполнены; SHA-1 ещё не добавлен в OAuth credential
- Лента: случайные 12 каналов из подписок → последние видео → перемешать
- Pull-to-refresh → другие 12 случайных каналов
- Квота YouTube API: ~390 ед./день из 10 000 — безопасно
- Тема: мягкие оттенки радуги, MD3, primary #5B9BD5, secondary #F4A261
- Светлая/тёмная тема: по системным настройкам
- Родительский контроль: умножение двух чисел 2-9
- Настройки: скрыты за burger menu [☰] в AppBar главного экрана
- Back gesture в плеере: заблокирован, выход только через кнопку ←
- Офлайн: только метаданные (кэш Room), видео только при интернете

## Статус Google Cloud Console

- [x] Проект создан, YouTube Data API v3 включён
- [x] OAuth consent screen: email ребёнка добавлен в Test Users
- [x] OAuth credential (Android) создан (без SHA-1)
- [x] SHA-1 добавлен в OAuth credential (Android) ✓

## Важные детали реализации

- `kotlin.android` плагин НЕ добавлять — конфликтует с AGP 9.x (уже встроен)
- `google-sign-in` → `google-signin` в TOML (избегаем keyword `in`)
- KSP 2.3.6 работает с любой версией Kotlin (начиная с KSP 2.3.0)
- Hilt требует минимум 2.59 для AGP 9.x
