package app.vigil.ui

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.vigil.BuildConfig
import app.vigil.R
import app.vigil.container
import app.vigil.data.Event
import app.vigil.data.Settings
import app.vigil.data.Urge
import app.vigil.data.Vow
import app.vigil.domain.Vows
import app.vigil.notify.Alarms
import app.vigil.ui.catalog.CatalogTab
import app.vigil.ui.catalog.PresetScreen
import app.vigil.ui.chronicle.ChronicleTab
import app.vigil.ui.chronicle.PageScreen
import app.vigil.ui.components.Candle
import app.vigil.ui.components.rememberNow
import app.vigil.ui.make.MakeScreen
import app.vigil.ui.onboarding.OnboardingScreen
import app.vigil.ui.release.ReleaseScreen
import app.vigil.ui.settings.SettingsScreen
import app.vigil.ui.temptation.TemptationScreen
import app.vigil.ui.theme.Alegreya
import app.vigil.ui.theme.U
import app.vigil.ui.vow.VowScreen
import app.vigil.ui.vows.VowsTab
import kotlinx.coroutines.launch

enum class Tab { VOWS, CATALOG, CHRONICLE }

/** Everything stored, recomputed once a minute or on change. */
@Immutable
class VigilData(val vows: List<Vow>, val events: List<Event>, val urges: List<Urge>, val settings: Settings, val now: Long) {
    val active: List<Vow> = vows.filter { it.active }.sortedBy { it.startAt }
    val running: List<Vow> = active.filter { Vows.started(it, now) }
    val waiting: List<Vow> = active.filter { !Vows.started(it, now) }
    val ended: List<Vow> = vows.filter { !it.active }.sortedByDescending { it.endedAt ?: it.startAt }

    /** The rule of three: room for another vow. */
    val room: Boolean get() = !settings.ruleOfThree || active.size < 3

    fun vow(id: Long) = vows.firstOrNull { it.id == id }
    fun eventsOf(v: Vow) = events.filter { it.vowId == v.id }
    fun urgesOf(v: Vow) = urges.filter { it.vowId == v.id }
}

/** Everything a screen can open. */
class Go(private val nav: NavHostController) {
    fun make(preset: String = "", start: Long = 0L, days: Int = 0, title: String = "") =
        nav.navigate("make?preset=$preset&start=$start&days=$days&title=" + Uri.encode(title)) { launchSingleTop = true }
    fun preset(id: String) = nav.navigate("preset/$id")
    fun vow(id: Long) = nav.navigate("vow/$id") { launchSingleTop = true }
    fun tempt(id: Long = 0L) = nav.navigate("tempt/$id") { launchSingleTop = true }
    fun release(id: Long, confess: Int = 0) = nav.navigate("release/$id?confess=$confess") { launchSingleTop = true }
    fun page(id: Long) = nav.navigate("page/$id") { launchSingleTop = true }
    fun settings() = nav.navigate("settings")
    fun home() = nav.popBackStack("main", false)
    fun back() = nav.popBackStack()
}

@Composable
fun VigilRoot(settings: Settings) {
    val context = LocalContext.current
    val container = context.container
    val nav = rememberNavController()
    val go = remember(nav) { Go(nav) }
    var tab by rememberSaveable { mutableStateOf(Tab.VOWS) }
    val start = remember { if (settings.onboarded) "main" else "welcome" }
    val dao = container.db.dao()
    val vows by dao.observeVows().collectAsStateWithLifecycle(initialValue = null)
    val events by dao.observeEvents().collectAsStateWithLifecycle(initialValue = emptyList())
    val urges by dao.observeUrges().collectAsStateWithLifecycle(initialValue = emptyList())
    val minute = rememberNow(30_000)
    val data = remember(vows, events, urges, settings, minute) { VigilData(vows.orEmpty(), events, urges, settings, System.currentTimeMillis()) }
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route

    // Onboarded elsewhere (the demo seed): leave the welcome screens.
    LaunchedEffect(settings.onboarded, route) {
        if (settings.onboarded && route == "welcome") nav.navigate("main") { popUpTo("welcome") { inclusive = true } }
    }
    // A vow fulfilled (or burned) since last time: its last page, once.
    LaunchedEffect(vows != null, route, data.ended.firstOrNull { !it.endSeen }?.id) {
        val fresh = data.ended.firstOrNull { !it.endSeen }
        if (vows != null && route == "main" && settings.onboarded && fresh != null) go.page(fresh.id)
    }

    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        container.scope.launch { container.changed() }
    }
    LaunchedEffect(settings.onboarded, data.active.isNotEmpty()) {
        if (settings.onboarded && data.active.isNotEmpty() && Build.VERSION.SDK_INT >= 33 && !Alarms.canNotify(context) && !BuildConfig.DEMO) {
            askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    CompositionLocalProvider(LocalGender provides settings.gender) {
        NavHost(
            navController = nav,
            startDestination = start,
            modifier = Modifier.background(U.c.bg),
            enterTransition = { fadeIn(tween(220)) },
            exitTransition = { fadeOut(tween(160)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(160)) },
        ) {
            composable("welcome") {
                OnboardingScreen { nav.navigate("main") { popUpTo("welcome") { inclusive = true } } }
            }
            composable("main") { MainTabs(tab, { tab = it }, go, data) }
            composable(
                "make?preset={preset}&start={start}&days={days}&title={title}",
                arguments = listOf(
                    navArgument("preset") { type = NavType.StringType; defaultValue = "" },
                    navArgument("start") { type = NavType.LongType; defaultValue = 0L },
                    navArgument("days") { type = NavType.IntType; defaultValue = 0 },
                    navArgument("title") { type = NavType.StringType; defaultValue = "" },
                ),
            ) { e ->
                MakeScreen(
                    e.arguments?.getString("preset").orEmpty(), e.arguments?.getLong("start") ?: 0L, e.arguments?.getInt("days") ?: 0,
                    e.arguments?.getString("title").orEmpty(), data, go,
                )
            }
            composable("preset/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { e ->
                PresetScreen(e.arguments?.getString("id").orEmpty(), data, go)
            }
            composable("vow/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                VowScreen(e.arguments?.getLong("id") ?: 0L, data, go)
            }
            composable("tempt/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                TemptationScreen(e.arguments?.getLong("id") ?: 0L, data, go)
            }
            composable(
                "release/{id}?confess={confess}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }, navArgument("confess") { type = NavType.IntType; defaultValue = 0 }),
            ) { e ->
                ReleaseScreen(e.arguments?.getLong("id") ?: 0L, e.arguments?.getInt("confess") ?: 0, data, go)
            }
            composable("page/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                PageScreen(e.arguments?.getLong("id") ?: 0L, data, go)
            }
            composable("settings") { SettingsScreen(data, go) }
        }
    }
}

@Composable
private fun MainTabs(tab: Tab, onTab: (Tab) -> Unit, go: Go, data: VigilData) {
    Column(Modifier.fillMaxSize().background(U.c.bg)) {
        Box(Modifier.weight(1f)) {
            when (tab) {
                Tab.VOWS -> VowsTab(data, go)
                Tab.CATALOG -> CatalogTab(data, go)
                Tab.CHRONICLE -> ChronicleTab(data, go)
            }
        }
        HorizontalDivider(color = U.c.line)
        Box(Modifier.fillMaxWidth().background(U.c.surface).navigationBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().height(66.dp).padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NavItem(Icons.Rounded.Verified, stringResource(R.string.tab_vows), tab == Tab.VOWS) { onTab(Tab.VOWS) }
                NavItem(Icons.AutoMirrored.Rounded.MenuBook, stringResource(R.string.tab_catalog), tab == Tab.CATALOG) { onTab(Tab.CATALOG) }
                Spacer(Modifier.weight(1f))
                NavItem(Icons.Rounded.AutoStories, stringResource(R.string.tab_chronicle), tab == Tab.CHRONICLE) { onTab(Tab.CHRONICLE) }
                NavItem(Icons.Rounded.Settings, stringResource(R.string.tab_settings), false) { go.settings() }
            }
            CenterButton(data, go, Modifier.align(Alignment.TopCenter).offset(y = (-20).dp))
        }
    }
}

/** With a vow running: the candle against temptation. Without one: a new vow. */
@Composable
private fun CenterButton(data: VigilData, go: Go, modifier: Modifier) {
    val tempt = data.running.isNotEmpty()
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).clickable { if (tempt) go.tempt() else go.make() },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(66.dp)
                .shadow(10.dp, CircleShape, ambientColor = U.c.rubric, spotColor = U.c.rubric)
                .clip(CircleShape)
                .background(if (tempt) Brush.radialGradient(listOf(Color(0xFF3A2A1E), U.c.night)) else Brush.radialGradient(listOf(U.c.rubric, U.c.rubric)))
                .border(2.dp, if (tempt) U.c.gold else U.c.rubricSoft, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (tempt) Candle(0.35f, Modifier.size(54.dp))
            else Icon(Icons.Rounded.Add, stringResource(R.string.new_vow), Modifier.size(32.dp), tint = U.c.onRubric)
        }
        Spacer(Modifier.height(2.dp))
        Text(
            stringResource(if (tempt) R.string.tempt_button else R.string.new_vow),
            fontFamily = Alegreya, fontSize = 12.sp, color = U.c.rubric, maxLines = 1,
        )
    }
}

@Composable
private fun RowScope.NavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) U.c.rubric else U.c.faint
    Column(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(14.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.clip(RoundedCornerShape(50)).background(if (selected) U.c.rubricSoft else Color.Transparent).padding(horizontal = 18.dp, vertical = 4.dp),
        ) { Icon(icon, null, Modifier.size(22.dp), tint = color) }
        Spacer(Modifier.height(3.dp))
        Text(label, fontFamily = Alegreya, fontSize = 12.sp, color = color, maxLines = 1)
    }
}
