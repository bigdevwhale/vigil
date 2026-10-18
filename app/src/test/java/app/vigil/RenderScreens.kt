package app.vigil

import android.graphics.Bitmap
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.hasScrollAction
import androidx.test.core.app.ApplicationProvider
import app.vigil.data.DemoSeed
import app.vigil.data.Settings
import app.vigil.ui.VigilRoot
import app.vigil.ui.theme.VigilTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Renders the main screens with the demo vows to PNGs (run with `-Prender`): a look without a phone. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h852dp-xhdpi")
class RenderScreens {
    @get:Rule
    val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val out = File(System.getProperty("render.out") ?: "build/art", "screens").apply { mkdirs() }

    private fun settle(ms: Long = 3000) {
        rule.mainClock.autoAdvance = false
        var t = 0L
        while (t < ms) {
            Thread.sleep(40)
            rule.mainClock.advanceTimeBy(100)
            t += 100
        }
    }

    private fun shot(name: String) {
        // Draw the window by hand: capturing waits for a frame, which a paused clock never gives.
        val root = rule.activity.window.decorView
        val b = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(android.graphics.Canvas(b))
        File(out, "$name.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun tap(text: String, substring: Boolean = true) {
        rule.onAllNodesWithText(text, substring = substring)[0].performClick()
        settle(2000)
    }

    private fun back() {
        rule.onAllNodesWithContentDescription("Back")[0].performClick()
        settle(1500)
    }

    /** Scrolls the screen's first scrollable by [px]; the scroll-to-node helper waits for frames a paused clock never gives. */
    private fun scroll(px: Float) {
        rule.onAllNodes(hasScrollAction())[0].performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, px) }
        settle(1500)
    }

    @Test
    fun screens() {
        val app = ApplicationProvider.getApplicationContext<VigilApp>()
        runBlocking { DemoSeed.seedIfEmpty(app, app.container) }
        rule.setContent {
            val s: Settings? by app.container.settings.settings.collectAsState(initial = null)
            VigilTheme { s?.let { VigilRoot(it) } }
        }
        settle(5000)
        shot("1-vows")
        tap("No sugar")
        shot("2-vow")
        scroll(1400f)
        shot("2b-vow-lower")
        back()
        tap("Temptation")
        shot("3-choose")
        tap("No sugar")
        settle(4000)
        shot("4-temptation")
        back()
        tap("Catalog")
        shot("5-catalog")
        tap("No sugar")
        shot("6-preset")
        tap("Take this vow")
        shot("7-make")
        back()
        back()
        tap("Chronicle")
        shot("8-chronicle")
        scroll(1600f)
        shot("8b-chronicle-lower")
        tap("No alcohol")
        settle(2000)
        shot("9-page")
    }

    @Test
    fun release() {
        val app = ApplicationProvider.getApplicationContext<VigilApp>()
        runBlocking { DemoSeed.seedIfEmpty(app, app.container) }
        rule.setContent {
            val s: Settings? by app.container.settings.settings.collectAsState(initial = null)
            VigilTheme { s?.let { VigilRoot(it) } }
        }
        settle(5000)
        tap("No sugar")
        tap("I want to break it")
        shot("10-ladder")
        back()
        tap("I already broke it")
        shot("11-outcome")
    }
}
