package app.vigil

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vigil.data.Settings
import app.vigil.ui.VigilRoot
import app.vigil.ui.theme.VigilTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Parchment is light: dark ink for the bars.
        val style = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        setContent {
            val settings: Settings? by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
            VigilTheme {
                // Wait for settings so the welcome screen doesn't flash for someone who already keeps vows.
                settings?.let { VigilRoot(it) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        container.write { }
    }
}
