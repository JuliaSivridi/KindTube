package io.github.juliasivridi.kindtube.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import io.github.juliasivridi.kindtube.ui.common.VideoCard
import io.github.juliasivridi.kindtube.ui.theme.LocalTabColor
import io.github.juliasivridi.kindtube.util.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    contentPadding: PaddingValues = PaddingValues(),
    onVideoClick: (videoId: String) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear history?") },
            text = { Text("All watched videos will be removed from history.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearHistory()
                    showClearDialog = false
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("History") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LocalTabColor.current),
                actions = {
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear history")
                    }
                },
            )
        }
    ) { scaffoldPadding ->
        val combined = PaddingValues(
            top = scaffoldPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding(),
        )
        when (val state = uiState) {
            is UiState.Loading -> LoadingView(modifier = Modifier.padding(combined))
            is UiState.Empty -> EmptyView(
                message = "Your watch history is empty",
                modifier = Modifier.padding(combined),
            )
            is UiState.Error -> ErrorView(
                errorType = state.type,
                modifier = Modifier.padding(combined),
            )
            is UiState.Success -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 200.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(combined),
                contentPadding = PaddingValues(horizontal = 50.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                items(state.data, key = { it.id }) { entry ->
                    VideoCard(
                        video = entry.video,
                        onClick = { onVideoClick(entry.video.id) },
                    )
                }
            }
        }
    }
}
