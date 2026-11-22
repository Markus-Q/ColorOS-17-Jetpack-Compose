package dev.irisui.catalog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Optional: `adb shell am start -n dev.irisui.catalog/.MainActivity --es destination PrimaryButton`
        // opens a demo screen directly (handy for testing a single component).
        val start = intent.getStringExtra("destination")
            ?.let { name -> CatalogDestination.entries.firstOrNull { it.name.equals(name, ignoreCase = true) } }
        // The blur lab reads more extras (background image, element, material), see BlurLabConfig.
        dev.irisui.catalog.demos.BlurLabConfig.extras = intent.extras
        // `--ez light true` shows the kit in its light theme whatever the system theme is (for comparing both modes)
        val forced = if (intent.hasExtra("light")) !intent.getBooleanExtra("light", false) else null
        // `--ez touches true` draws a circle under the finger, so screen recordings show what is being touched
        val touches = intent.getBooleanExtra("touches", false)
        setContent { CatalogApp(start, forced, touches) }
        if (intent.getBooleanExtra("perm", false)) {
            window.decorView.postDelayed({ requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 7) }, 1200)
        }
    }
}
