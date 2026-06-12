package io.github.juliasivridi.kindtube.ui.navigation

sealed class Screen(val route: String) {
    // Bottom nav tabs
    object Home : Screen("home")
    object History : Screen("history")
    object Subscriptions : Screen("subscriptions")
    object Search : Screen("search")

    // Detail screens (no bottom nav)
    object Player : Screen("player/{videoId}") {
        fun createRoute(videoId: String) = "player/$videoId"
    }
    object Channel : Screen("channel/{channelId}/{uploadsPlaylistId}") {
        fun createRoute(channelId: String, uploadsPlaylistId: String) =
            "channel/$channelId/$uploadsPlaylistId"
    }
    object Settings : Screen("settings")
}
