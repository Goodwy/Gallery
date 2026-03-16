# CLAUDE.md

This file provides guidance to AI Agents when working with code in this repository.

## Project Overview

Right Gallery is an Android gallery/photo viewer app (package: `com.goodwy.gallery`), forked from Simple Gallery. Written in Kotlin, it uses View Binding (not Compose), Room for local database, Glide for image loading, and ExoPlayer (Media3) for video playback.

## Build Commands

```bash
# Debug build (FOSS flavor)
./gradlew assembleFossDebug

# Debug build (Google Play flavor)
./gradlew assembleGplayDebug

# Release build
./gradlew assembleFossRelease
./gradlew assembleGplayRelease

# Lint check
./gradlew lint

# Detekt (static analysis)
./gradlew detekt
```

## Build Configuration

- **Min SDK**: 26, **Target SDK**: 34, **Compile SDK**: 35
- **Java/Kotlin target**: JVM 17
- **Product flavors**: `foss` and `gplay` (licensing dimension) — flavor-specific resources live in `app/src/foss/` and `app/src/gplay/`
- **Signing**: via `keystore.properties` file or `SIGNING_*` environment variables
- **In-app product IDs**: configured in `local.properties` (PRODUCT_ID_X1..X4, SUBSCRIPTION_ID_X1..X3, etc.)
- **Version info**: managed in `gradle.properties` (VERSION_NAME, VERSION_CODE, APP_ID)
- **Dependencies catalog**: `gradle/libs.versions.toml`

## Architecture

### Core dependency
The app depends heavily on `com.github.Goodwy:Goodwy-Commons` (referenced as `goodwy.commons`), which provides base activities, shared utilities, common UI components, and the commons configuration system. Many base classes (activities, helpers) come from this library.

### Key packages under `app/src/main/kotlin/com/goodwy/gallery/`

- **activities/**: All Activity classes. `SimpleActivity` is the base for most activities. Key activities: `MainActivity` (directory listing), `MediaActivity` (media grid within a folder), `ViewPagerActivity` (full-screen media viewer), `EditActivity` (image editor), `VideoPlayerActivity`
- **adapters/**: RecyclerView adapters — `DirectoryAdapter` (folder list), `MediaAdapter` (media grid), with custom binding helpers (`DirectoryItemBinding`, `MediaItemBinding`)
- **models/**: Room entities and data classes — `Directory`, `Medium`, `Favorite`, `DateTaken`, `Widget`, plus editor models (`CanvasOp`, `PaintOptions`, `MyPath`)
- **databases/**: `GalleryDatabase` — Room database (version 10) with DAOs for directories, media, widgets, date_takens, and favorites
- **interfaces/**: Room DAO interfaces (`DirectoryDao`, `MediumDao`, `FavoritesDao`, `DateTakensDao`, `WidgetsDao`) and listener interfaces
- **helpers/**: `Config` (SharedPreferences wrapper for app settings), `Constants` (shared constant values), `MediaFetcher` (scans filesystem for media files), image loading helpers
- **extensions/**: Kotlin extension functions on `Context`, `Activity`, `String`, `View`, etc.
- **fragments/**: `PhotoFragment` and `VideoFragment` for the ViewPager-based media viewer
- **views/**: Custom views — `EditorDrawCanvas`, `MediaSideScroll` (brightness/volume gesture), `InstantItemSwitch`
- **receivers/**: `BootCompletedReceiver`, `RefreshMediaReceiver`
- **jobs/**: `NewPhotoFetcher` (background job to detect new photos)

### Data flow
Media discovery flows through `MediaFetcher` → cached in Room (`MediumDao`/`DirectoryDao`) → displayed via adapters. The `Config` helper wraps SharedPreferences for all user settings (sort order, view type, grouping, etc.).
