package io.github.juliasivridi.kindtube.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import io.github.juliasivridi.kindtube.domain.model.BlockedChannel
import io.github.juliasivridi.kindtube.ui.common.ParentalControlDialog
import io.github.juliasivridi.kindtube.ui.theme.LocalTabColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val blockedChannels by viewModel.blockedChannels.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ── Channel list backup: export / import JSON via the system file picker ──
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val json = viewModel.buildExportJson()
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use {
                        it.write(json.toByteArray())
                    } != null
                }.getOrDefault(false)
            }
            Toast.makeText(
                context,
                if (ok) "Channels exported" else "Export failed",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val json = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use {
                        it.readBytes().decodeToString()
                    }
                }.getOrNull()
            }
            val imported = if (json != null) viewModel.importChannelsJson(json) else -1
            Toast.makeText(
                context,
                if (imported >= 0) "Imported $imported channels" else "Import failed: not a KindTube backup",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    // Parental control to unblock a channel
    var channelToUnblock by remember { mutableStateOf<BlockedChannel?>(null) }
    if (channelToUnblock != null) {
        ParentalControlDialog(
            onSuccess = {
                viewModel.unblockChannel(channelToUnblock!!.channelId)
                channelToUnblock = null
            },
            onDismiss = { channelToUnblock = null },
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LocalTabColor.current),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {

            // ── Blocked channels header ──
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Text(
                        "Blocked channels (${blockedChannels.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                    HorizontalDivider()
                }
            }

            if (blockedChannels.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "No blocked channels",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            } else {
                items(blockedChannels, key = { it.channelId }) { channel ->
                    BlockedChannelGridItem(
                        channel = channel,
                        onUnblockRequest = { channelToUnblock = channel },
                    )
                }
            }

            // ── Other settings ──
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = viewModel::clearCache,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(),
                    ) {
                        Text("Force refresh feed")
                    }
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Channel list backup",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                                exportLauncher.launch("kindtube-channels-$date.json")
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Export JSON")
                        }
                        Button(
                            onClick = { importLauncher.launch(arrayOf("application/json")) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Import JSON")
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Text(
                        "Version 1.1.0",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun BlockedChannelGridItem(
    channel: BlockedChannel,
    onUnblockRequest: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        // Avatar — NOT clickable (blocked)
        AsyncImage(
            model = channel.channelAvatarUrl,
            contentDescription = channel.channelTitle,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
        )

        // Name + ⋮
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = channel.channelTitle,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 2.dp),
            )
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        Icons.Filled.MoreVert, null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Unblock") },
                        leadingIcon = {
                            Icon(Icons.Filled.LockOpen, null,
                                tint = MaterialTheme.colorScheme.primary)
                        },
                        onClick = {
                            menuExpanded = false
                            onUnblockRequest()
                        },
                    )
                }
            }
        }
    }
}
