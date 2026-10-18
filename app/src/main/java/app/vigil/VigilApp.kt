package app.vigil

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import app.vigil.data.DemoSeed
import app.vigil.data.Keeper
import app.vigil.data.SettingsStore
import app.vigil.data.VigilDatabase
import app.vigil.data.Voice
import app.vigil.notify.Alarms
import app.vigil.widget.CandleWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(val context: Context) {
    val db = VigilDatabase.create(context)
    val settings = SettingsStore(context)
    val keeper = Keeper(db)
    val voice = Voice(context)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Fire-and-forget so a write survives the screen being closed right after tapping. */
    fun write(block: suspend AppContainer.() -> Unit) {
        scope.launch {
            block()
            changed()
        }
    }

    /** Fulfil what has run its course, re-arm the notices and repaint the candle on the home screen. */
    suspend fun changed() {
        keeper.sweep()
        Alarms.reschedule(context)
        CandleWidget.refresh(context)
    }
}

class VigilApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Alarms.ensureChannels(this)
        // Re-arm on every start: OEM task killers (HyperOS & co.) silently drop alarms.
        container.scope.launch {
            if (BuildConfig.DEMO) DemoSeed.seedIfEmpty(this@VigilApp, container)
            container.changed()
        }
    }
}

val Context.container: AppContainer get() = (applicationContext as VigilApp).container

/** Resources in the in-app language, for receivers that AppCompat doesn't localise before Android 13. */
fun Context.localized(): Context {
    val tags = AppCompatDelegate.getApplicationLocales()
    if (tags.isEmpty) return this
    val config = Configuration(resources.configuration)
    config.setLocale(tags[0])
    return createConfigurationContext(config)
}
