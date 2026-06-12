package io.github.juliasivridi.kindtube.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import io.github.juliasivridi.kindtube.ui.common.EmptyView
import io.github.juliasivridi.kindtube.ui.common.ErrorView
import io.github.juliasivridi.kindtube.ui.common.LoadingView
import io.github.juliasivridi.kindtube.ui.common.ParentalControlDialog
import io.github.juliasivridi.kindtube.ui.common.VideoCard
import io.github.juliasivridi.kindtube.ui.theme.LocalTabColor
import io.github.juliasivridi.kindtube.util.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    contentPadding: PaddingValues = PaddingValues(),
    onVideoClick: (videoId: String) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    // Parental control gate for Settings
    var showParentalControl by remember { mutableStateOf(false) }

    if (showParentalControl) {
        ParentalControlDialog(
            onSuccess = {
                showParentalControl = false
                onSettingsClick()
            },
            onDismiss = { showParentalControl = false },
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("MyKidTube") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LocalTabColor.current),
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                    // ☰ — hidden entry point to Settings, guarded by parental control
                    IconButton(onClick = { showParentalControl = true }) {
                        Icon(Icons.Filled.Menu, contentDescription = "Settings")
                    }
                },
            )
        }
    ) { scaffoldPadding ->
        // Combine scaffold padding with bottom nav padding
        val combinedPadding = PaddingValues(
            top = scaffoldPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding(),
        )
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(combinedPadding),
        ) {
            when (val state = uiState) {
                is UiState.Loading -> LoadingView()
                is UiState.Empty -> EmptyView(
                    "No videos.\nAdd channels in the Subscriptions tab!"
                )
                is UiState.Error -> ErrorView(
                    errorType = state.type,
                    onRetry = viewModel::refresh,
                )
                is UiState.Success -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 200.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 50.dp, vertical = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        items(state.data, key = { it.id }) { video ->
                            VideoCard(
                                video = video,
                                onClick = { onVideoClick(video.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}
