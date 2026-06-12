package io.github.juliasivridi.kindtube

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.juliasivridi.kindtube.ui.navigation.NavGraph
import io.github.juliasivridi.kindtube.ui.theme.MyKidTubeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyKidTubeTheme {
                NavGraph()
            }
        }
    }
}
