package io.github.juliasivridi.kindtube.ui.player

import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import io.github.juliasivridi.kindtube.domain.model.Video
import io.github.juliasivridi.kindtube.ui.common.ParentalControlDialog
import kotlinx.coroutines.delay

private const val OVERLAY_HIDE_DELAY_MS = 3_000L

/**
 * Injected into the embed page once it loads. Because the embed is the MAIN
 * frame (not an iframe), its DOM is same-origin for us: we hide all YouTube
 * chrome with CSS (big play/pause button, channel banner, pause overlay,
 * watermark, end screen) and control the <video> element directly — no
 * IFrame API handshakes that the player can ignore.
 */
private val PLAYER_SETUP_JS = """
(function() {
  if (window._kidtube) return;
  window._kidtube = true;
  // Hide EVERYTHING except the <video> itself. Class names differ between
  // YouTube's desktop and mobile embed players (and change over time), so
  // instead of chasing them we make all chrome invisible: visibility is
  // overridable by descendants, so the nested <video> stays visible while
  // every overlay (title, subscribe, big play/pause, end screen) disappears.
  var st = document.createElement('style');
  st.textContent =
    'body *{visibility:hidden!important}' +
    'video{visibility:visible!important}';
  document.head.appendChild(st);
  window._v = function() { return document.querySelector('video'); };
  window._play  = function() { var v = _v(); if (v) v.play(); };
  window._pause = function() { var v = _v(); if (v) v.pause(); };
  window._seek  = function(s) { var v = _v(); if (v) v.currentTime = s; };
  window._poll  = function() {
    var v = _v();
    if (!v || !v.duration) return [-1, 0, -1];
    var state = v.ended ? 0 : (v.paused ? 2 : 1);
    return [v.currentTime / v.duration, v.duration, state];
  };
})();
""".trimIndent()

/** Format seconds as mm:ss (e.g. 125f → "02:05"). */
private fun formatTime(seconds: Float): String {
    val s = seconds.toInt().coerceAtLeast(0)
    return "%02d:%02d".format(s / 60, s % 60)
}

@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    onChannelBlocked: () -> Unit,
    onVideoClick: (videoId: String) -> Unit = {},
    onChannelClick: (channelId: String) -> Unit = {},
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val video by viewModel.video.collectAsState()
    val relatedVideos by viewModel.relatedVideos.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val videoId = viewModel.videoId

    // Parental control gate for block channel
    var showParentalControl by remember { mutableStateOf(false) }
    if (showParentalControl) {
        ParentalControlDialog(
            onSuccess = {
                showParentalControl = false
                viewModel.blockChannel()
                onChannelBlocked()
            },
            onDismiss = { showParentalControl = false },
        )
    }

    // WebView lifecycle
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> webViewRef.value?.onPause()
                Lifecycle.Event.ON_RESUME -> webViewRef.value?.onResume()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            webViewRef.value?.destroy()
        }
    }

    // Top overlay auto-hides after 3 s.
    // overlayResetKey lets us restart the 3-second timer from interactions (seek, play/pause)
    // without toggling overlayVisible off and on.
    var overlayVisible by remember { mutableStateOf(true) }
    var overlayResetKey by remember { mutableIntStateOf(0) }

    // Progress 0f..1f and total duration in seconds — polled from WebView
    val progressState = remember { mutableFloatStateOf(0f) }
    var progress by progressState
    val durationState = remember { mutableFloatStateOf(0f) }
    var durationSec by durationState

    // Playback state tracked locally; playerState mirrors YT.PlayerState
    // (-1 unstarted, 0 ended, 1 playing, 2 paused) polled from the page
    var isPaused by remember { mutableStateOf(false) }
    var playerState by remember { mutableIntStateOf(-1) }
    val ended = playerState == 0

    // Related videos panel — declared before LaunchedEffect so it can be referenced there
    var panelVisible by remember { mutableStateOf(false) }

    LaunchedEffect(overlayVisible, overlayResetKey, ended, isPaused) {
        // Pinned while paused or ended (YouTube Kids style): the kid needs the
        // play/replay button and the panel until playback resumes
        if (overlayVisible && !ended && !isPaused) {
            delay(OVERLAY_HIDE_DELAY_MS)
            overlayVisible = false
            panelVisible = false  // hide panel together with the rest of the overlay
        }
    }

    // Video ended → surface our own end overlay with replay + related videos
    LaunchedEffect(ended) {
        if (ended) {
            overlayVisible = true
            if (relatedVideos.isNotEmpty()) panelVisible = true
        }
    }

    // ── Helper functions ──────────────────────────────────────────────────────

    /** Run JS in the wrapper page (calls into the YT.Player created there). */
    fun runJs(js: String) {
        webViewRef.value?.evaluateJavascript(js, null)
    }

    fun seekTo(fraction: Float) {
        runJs("_seek(${(fraction * durationSec).toInt()})")
    }

    fun replay() {
        runJs("_seek(0);_play()")
        progressState.floatValue = 0f
        playerState = 1  // optimistic; the next poll confirms
        isPaused = false
        panelVisible = false
        overlayResetKey++
    }

    fun togglePlayPause() {
        if (ended) {
            replay()
            return
        }
        if (isPaused) {
            runJs("_play()")
            isPaused = false
        } else {
            runJs("_pause()")
            isPaused = true
        }
        overlayResetKey++ // reset auto-hide timer on play/pause interaction
    }

    // ── Root ──────────────────────────────────────────────────────────────────

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {

        // ── Layer 1: thumbnail placeholder (shown while iframe loads) ──
        video?.let { v ->
            AsyncImage(
                model = v.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // ── Layer 2: WebView ──
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    settings.apply {
                        javaScriptEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        domStorageEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                    }
                    // Block all navigation — channel links, related videos, etc. —
                    // and inject the chrome-hiding / control script once loaded
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest,
                        ): Boolean = true

                        override fun onPageFinished(view: WebView, url: String?) {
                            view.evaluateJavascript(PLAYER_SETUP_JS, null)
                        }
                    }
                    // Block popups / new windows
                    webChromeClient = object : WebChromeClient() {
                        override fun onCreateWindow(
                            view: WebView, isDialog: Boolean,
                            isUserGesture: Boolean, resultMsg: android.os.Message?,
                        ): Boolean = false
                    }
                    // The embed page is loaded as the MAIN frame so that
                    // PLAYER_SETUP_JS (injected in onPageFinished) can reach its DOM.
                    // The Referer header is required: without it YouTube treats the
                    // embed context as invalid and shows error 153 ("Watch on YouTube").
                    val embedUrl = "https://www.youtube-nocookie.com/embed/$videoId" +
                        "?autoplay=1&controls=0&rel=0&modestbranding=1" +
                        "&iv_load_policy=3&playsinline=1&fs=0&disablekb=1"
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    loadUrl(embedUrl, mapOf("Referer" to "https://www.youtube-nocookie.com/"))
                    webViewRef.value = this
                }
            },
            modifier = Modifier.matchParentSize(),
        )

        // ── Progress polling: [fraction, durationSeconds, playerState] as JS array ──
        LaunchedEffect(webViewRef.value) {
            val webView = webViewRef.value ?: return@LaunchedEffect
            while (true) {
                delay(500)
                webView.evaluateJavascript("_poll()") { result ->
                    val cleaned = result
                        ?.removePrefix("[")
                        ?.removeSuffix("]")
                        ?: return@evaluateJavascript
                    val parts = cleaned.split(",")
                    val f = parts.getOrNull(0)?.toFloatOrNull() ?: -1f
                    val dur = parts.getOrNull(1)?.toFloatOrNull() ?: 0f
                    val state = parts.getOrNull(2)?.toFloatOrNull()?.toInt() ?: -1
                    if (f in 0f..1f) {
                        progressState.floatValue = f
                        if (dur > 0f) durationState.floatValue = dur
                    }
                    // Sync local flags with the real player state so the UI never lies
                    playerState = state
                    when (state) {
                        1 -> isPaused = false   // playing
                        2 -> isPaused = true    // paused
                    }
                }
            }
        }

        // ── Layer 3: full-size touch interceptor + all UI overlays ──
        Box(
            modifier = Modifier
                .matchParentSize()
                // Tap the video area → toggle overlay; also open the panel when showing
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        // Tap toggles EVERYTHING: controls + wide progress bar + panel
                        val newVisible = !overlayVisible
                        overlayVisible = newVisible
                        panelVisible = newVisible && relatedVideos.isNotEmpty()
                    },
                )
                // Swipe down anywhere → close panel
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        if (dragAmount > 30f && panelVisible) panelVisible = false
                    }
                },
        ) {

            // ── End cover: thumbnail + big Replay (like YouTube Kids) ──
            if (ended) {
                Box(modifier = Modifier.matchParentSize()) {
                    video?.let { v ->
                        AsyncImage(
                            model = v.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize(),
                        )
                    }
                    Box(
                        Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = 0.45f)),
                    )
                    IconButton(
                        onClick = { replay() },
                        modifier = Modifier
                            .size(88.dp)
                            .align(Alignment.Center)
                            .background(Color.Black.copy(alpha = 0.55f), CircleShape),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Replay,
                            contentDescription = "Replay",
                            tint = Color.White,
                            modifier = Modifier.size(56.dp),
                        )
                    }
                }
            }

            // ── Top overlay (back / title / block) ──
            AnimatedVisibility(
                visible = overlayVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(88.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.72f), Color.Transparent),
                            ),
                        ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                                // Tap on the title/channel area → open the channel screen
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { video?.let { onChannelClick(it.channelId) } },
                        ) {
                            video?.let { v ->
                                Text(
                                    v.channelTitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    v.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        IconButton(onClick = { showParentalControl = true }) {
                            Icon(Icons.Filled.Block, "Block channel", tint = Color.White)
                        }
                    }
                }
            }

            // ── Bottom column: [controls / thin bar] then [panel] ──
            // Panel is BELOW the progress bar, like YouTube Kids.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
            ) {

                // Enhanced controls row — visible when overlay shown.
                // Uses pointerInput to consume events so parent clickable doesn't toggle
                // the overlay when the user touches the seekbar or play/pause button.
                AnimatedVisibility(
                    visible = overlayVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.60f))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            // No-op clickable: swallows taps on the bar background so the
                            // parent clickable doesn't toggle the overlay, while child
                            // buttons still receive their clicks (a consume-everything
                            // pointerInput here would cancel the children's clickables).
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { overlayResetKey++ },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Play / Pause button — extra large for kids (YouTube Kids style)
                        IconButton(
                            onClick = { togglePlayPause() },
                            modifier = Modifier.size(64.dp),
                        ) {
                            Icon(
                                imageVector = when {
                                    ended -> Icons.Filled.Replay
                                    isPaused -> Icons.Filled.PlayArrow
                                    else -> Icons.Filled.Pause
                                },
                                contentDescription = when {
                                    ended -> "Replay"
                                    isPaused -> "Play"
                                    else -> "Pause"
                                },
                                tint = Color.White,
                                modifier = Modifier.size(48.dp),
                            )
                        }

                        Spacer(Modifier.width(4.dp))

                        // Seek bar — 10dp track, draggable white-circle thumb.
                        // onSeek also resets the auto-hide timer so the overlay doesn't
                        // disappear mid-drag.
                        VideoSeekBar(
                            progress = progress,
                            onSeek = { fraction ->
                                progressState.floatValue = fraction
                                seekTo(fraction)
                                overlayResetKey++ // restart the 3-second timer
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp),
                        )

                        Spacer(Modifier.width(8.dp))

                        // Time display: current / total
                        Text(
                            text = "${formatTime(progress * durationSec)} / ${formatTime(durationSec)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            modifier = Modifier.width(96.dp),
                        )
                    }
                }

                // Thin progress bar — shown when overlay is hidden
                AnimatedVisibility(
                    visible = !overlayVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = Color.Red,
                        trackColor = Color.White.copy(alpha = 0.25f),
                    )
                }

                // Related videos panel — BELOW the progress bar, slides up from the bottom.
                // Consuming pointer events in the panel prevents the parent clickable
                // from accidentally toggling the overlay when the user taps inside the panel.
                AnimatedVisibility(
                    visible = panelVisible,
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it },
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.90f))
                            // No-op clickable: keeps taps on the panel background from
                            // toggling the overlay without cancelling the child cards
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {}
                            // Swipe down → close panel
                            .pointerInput(Unit) {
                                detectVerticalDragGestures { _, dragAmount ->
                                    if (dragAmount > 30f) panelVisible = false
                                }
                            },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "More videos",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { panelVisible = false }) {
                                Icon(Icons.Filled.KeyboardArrowDown, null, tint = Color.White)
                            }
                        }

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(relatedVideos, key = { it.id }) { related ->
                                CompactVideoCard(
                                    video = related,
                                    onClick = { onVideoClick(related.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Seek bar drawn on Canvas: 10dp red track, white circle thumb.
 * Supports tap-to-seek and drag-to-seek.
 * Events are consumed internally so they don't bubble to parent.
 */
@Composable
private fun VideoSeekBar(
    progress: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .height(40.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    onSeek((down.position.x / size.width).coerceIn(0f, 1f))
                    drag(down.id) { change ->
                        change.consume()
                        onSeek((change.position.x / size.width).coerceIn(0f, 1f))
                    }
                }
            },
    ) {
        // Chunky track + thumb — easier for small fingers (YouTube Kids style)
        val trackH = 16.dp.toPx()
        val thumbR = 13.dp.toPx()
        val cy = size.height / 2f
        val startX = thumbR
        val trackW = (size.width - thumbR * 2f).coerceAtLeast(0f)

        // Track background
        drawRoundRect(
            color = Color.White.copy(alpha = 0.30f),
            topLeft = Offset(startX, cy - trackH / 2f),
            size = Size(trackW, trackH),
            cornerRadius = CornerRadius(trackH / 2f),
        )

        // Filled portion
        val fillW = (trackW * progress).coerceAtLeast(0f)
        if (fillW > 0f) {
            drawRoundRect(
                color = Color.Red,
                topLeft = Offset(startX, cy - trackH / 2f),
                size = Size(fillW, trackH),
                cornerRadius = CornerRadius(trackH / 2f),
            )
        }

        // White circle thumb
        drawCircle(
            color = Color.White,
            radius = thumbR,
            center = Offset(startX + trackW * progress, cy),
        )
    }
}

/** Compact card for the horizontal "related videos" panel. */
@Composable
private fun CompactVideoCard(video: Video, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(180.dp)
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = video.thumbnailUrl,
            contentDescription = video.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp)),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = video.title,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
