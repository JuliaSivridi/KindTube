package io.github.juliasivridi.kindtube.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.juliasivridi.kindtube.util.ErrorType

@Composable
fun ErrorView(
    errorType: ErrorType,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val icon: ImageVector
    val message: String
    when (errorType) {
        ErrorType.NO_NETWORK -> {
            icon = Icons.Filled.CloudOff
            message = "No internet.\nCheck your connection!"
        }
        ErrorType.QUOTA_EXCEEDED -> {
            icon = Icons.Filled.Lock
            message = "YouTube quota exceeded.\nTry again tomorrow"
        }
        ErrorType.AUTH_EXPIRED -> {
            icon = Icons.Filled.Lock
            message = "Login required"
        }
        ErrorType.GENERIC -> {
            icon = Icons.Filled.ErrorOutline
            message = "Something went wrong"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null) {
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}
