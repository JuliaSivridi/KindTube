package io.github.juliasivridi.kindtube.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.juliasivridi.kindtube.ui.channel.ChannelScreen
import io.github.juliasivridi.kindtube.ui.history.HistoryScreen
import io.github.juliasivridi.kindtube.ui.home.HomeScreen
import io.github.juliasivridi.kindtube.ui.player.PlayerScreen
import io.github.juliasivridi.kindtube.ui.search.SearchScreen
import io.github.juliasivridi.kindtube.ui.settings.SettingsScreen
import io.github.juliasivridi.kindtube.ui.subscriptions.SubscriptionsScreen
import io.github.juliasivridi.kindtube.ui.theme.GradientBottom
import io.github.juliasivridi.kindtube.ui.theme.GradientTop
import io.github.juliasivridi.kindtube.ui.theme.LocalTabColor

/**
 * Each tab has a rich icon color (visible on any background)
 * and a soft pastel bar color (used for TopAppBar + NavigationBar background).
 */
private data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
    val iconColor: Color,
    val barColor: Color,
)

private val bottomNavItems = listOf(
    BottomNavItem(
        Screen.Home, "Home", Icons.Filled.Home,
        iconColor = Color(0xFF1565C0),  // rich blue
        barColor  = Color(0xFFBBDEFB),  // soft blue
    ),
    BottomNavItem(
        Screen.History, "History", Icons.Filled.History,
        iconColor = Color(0xFFE65100),  // rich orange
        barColor  = Color(0xFFFFE0B2),  // soft orange
    ),
    BottomNavItem(
        Screen.Subscriptions, "Subscriptions", Icons.Filled.Subscriptions,
        iconColor = Color(0xFF2E7D32),  // rich green
        barColor  = Color(0xFFC8E6C9),  // soft green
    ),
    BottomNavItem(
        Screen.Search, "Search", Icons.Filled.Search,
        iconColor = Color(0xFFC62828),  // rich red
        barColor  = Color(0xFFFFCDD2),  // soft pink
    ),
)

private val tabRoutes = setOf(
    Screen.Home.route,
    Screen.History.route,
    Screen.Subscriptions.route,
    Screen.Search.route,
)

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val gradientBrush = Brush.verticalGradient(listOf(GradientTop, GradientBottom))

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Persist the bar/icon colors when navigating to non-tab screens
    var activeItem by remember { mutableStateOf(bottomNavItems.first()) }
    LaunchedEffect(currentRoute) {
        bottomNavItems.find { it.screen.route == currentRoute }?.let { activeItem = it }
    }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            if (currentRoute in tabRoutes) {
                // Centered icon row: equal margins on both sides, 50dp between icons
                NavigationBar(containerColor = activeItem.barColor) {
                    Spacer(Modifier.weight(1f))
                    bottomNavItems.forEachIndexed { index, item ->
                        if (index > 0) Spacer(Modifier.width(50.dp))
                        IconButton(
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        ) {
                            Icon(item.icon, contentDescription = null, tint = item.iconColor)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush),
        ) {
            // Provide the active bar color to all screens via CompositionLocal
            CompositionLocalProvider(LocalTabColor provides activeItem.barColor) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route,
                ) {
                    // ── Home tab ──
                    composable(Screen.Home.route) {
                        HomeScreen(
                            contentPadding = paddingValues,
                            onVideoClick = { navController.navigate(Screen.Player.createRoute(it)) },
                            onSearchClick = {
                                navController.navigate(Screen.Search.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onSettingsClick = { navController.navigate(Screen.Settings.route) },
                        )
                    }

                    // ── History tab ──
                    composable(Screen.History.route) {
                        HistoryScreen(
                            contentPadding = paddingValues,
                            onVideoClick = { navController.navigate(Screen.Player.createRoute(it)) },
                        )
                    }

                    // ── Subscriptions tab ──
                    composable(Screen.Subscriptions.route) {
                        SubscriptionsScreen(
                            onChannelClick = { channelId, uploadsPlaylistId ->
                                navController.navigate(Screen.Channel.createRoute(channelId, uploadsPlaylistId))
                            },
                        )
                    }

                    // ── Search tab ──
                    composable(Screen.Search.route) {
                        SearchScreen(
                            contentPadding = paddingValues,
                            onVideoClick = { navController.navigate(Screen.Player.createRoute(it)) },
                        )
                    }

                    // ── Player (fullscreen, no bottom nav) ──
                    composable(
                        route = Screen.Player.route,
                        arguments = listOf(navArgument("videoId") { type = NavType.StringType }),
                    ) {
                        PlayerScreen(
                            onBack = { navController.popBackStack() },
                            onChannelBlocked = {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        inclusive = false
                                    }
                                }
                            },
                            onVideoClick = { videoId ->
                                navController.navigate(Screen.Player.createRoute(videoId)) {
                                    popUpTo(Screen.Player.route) { inclusive = true }
                                }
                            },
                            onChannelClick = { channelId ->
                                // YouTube convention: the uploads playlist id is the
                                // channel id with the "UC" prefix replaced by "UU"
                                if (channelId.startsWith("UC")) {
                                    val uploadsPlaylistId = "UU" + channelId.removePrefix("UC")
                                    navController.navigate(
                                        Screen.Channel.createRoute(channelId, uploadsPlaylistId)
                                    )
                                }
                            },
                        )
                    }

                    // ── Channel screen (no bottom nav) ──
                    composable(
                        route = Screen.Channel.route,
                        arguments = listOf(
                            navArgument("channelId") { type = NavType.StringType },
                            navArgument("uploadsPlaylistId") { type = NavType.StringType },
                        ),
                    ) {
                        ChannelScreen(
                            onBack = { navController.popBackStack() },
                            onVideoClick = { navController.navigate(Screen.Player.createRoute(it)) },
                        )
                    }

                    // ── Settings (no bottom nav, guarded by parental control in HomeScreen) ──
                    composable(Screen.Settings.route) {
                        SettingsScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}
