package app.vigil.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vigil.R

/**
 * A scriptorium: parchment and iron-gall ink, a red rubric for what matters, gold and lapis for
 * illumination, and the night by the candle for temptation.
 */
@Immutable
data class Palette(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val line: Color,
    val ink: Color,
    val muted: Color,
    val faint: Color,
    val rubric: Color,
    val rubricSoft: Color,
    val onRubric: Color,
    val gold: Color,
    val goldSoft: Color,
    val lapis: Color,
    val lapisSoft: Color,
    val verdigris: Color,
    val verdigrisSoft: Color,
    val night: Color,
    val night2: Color,
    val nightInk: Color,
    val nightMuted: Color,
    val flame: Color,
)

val Parchment = Palette(
    bg = Color(0xFFEFE3CA),
    surface = Color(0xFFF8F0DF),
    surface2 = Color(0xFFE7D8B9),
    line = Color(0xFFD5C29C),
    ink = Color(0xFF2B1D14),
    muted = Color(0xFF6B5644),
    faint = Color(0xFF9C8569),
    rubric = Color(0xFFA8322A),
    rubricSoft = Color(0xFFF1D5C6),
    onRubric = Color(0xFFFFF6E8),
    gold = Color(0xFFB8892B),
    goldSoft = Color(0xFFF2E1B4),
    lapis = Color(0xFF2F4C86),
    lapisSoft = Color(0xFFDDE2EC),
    verdigris = Color(0xFF3E7A68),
    verdigrisSoft = Color(0xFFD8E7DD),
    night = Color(0xFF130D0A),
    night2 = Color(0xFF221812),
    nightInk = Color(0xFFF3E3C4),
    nightMuted = Color(0xFFB59D7E),
    flame = Color(0xFFFFB238),
)

val LocalPalette = staticCompositionLocalOf { Parchment }

/** The interface: a humanist sans with a pen's warmth. */
val Alegreya = FontFamily(
    Font(R.font.alegreyasans_regular, FontWeight.Normal),
    Font(R.font.alegreyasans_medium, FontWeight.Medium),
    Font(R.font.alegreyasans_bold, FontWeight.Bold),
)

/** Titles and the vow's own words. */
val Cormorant = FontFamily(
    Font(R.font.cormorant_semibold, FontWeight.SemiBold),
    Font(R.font.cormorant_bold, FontWeight.Bold),
    Font(R.font.cormorant_italic, FontWeight.Medium, FontStyle.Italic),
)

/** Illuminated initials, in the old Slavonic manner. */
val Ruslan = FontFamily(Font(R.font.ruslan, FontWeight.Normal))

object U {
    val c: Palette @Composable get() = LocalPalette.current

    val RXl = RoundedCornerShape(26.dp)
    val RLg = RoundedCornerShape(20.dp)
    val RMd = RoundedCornerShape(14.dp)
    val RSm = RoundedCornerShape(10.dp)
}

object UType {
    val Display: TextStyle @Composable get() = TextStyle(fontFamily = Cormorant, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 38.sp, color = U.c.ink, fontFeatureSettings = "lnum")
    val Title: TextStyle @Composable get() = TextStyle(fontFamily = Cormorant, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 30.sp, color = U.c.ink, fontFeatureSettings = "lnum")
    val Name: TextStyle @Composable get() = TextStyle(fontFamily = Cormorant, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 24.sp, color = U.c.ink, fontFeatureSettings = "lnum")
    val Sub: TextStyle @Composable get() = TextStyle(fontFamily = Alegreya, fontSize = 15.5.sp, lineHeight = 21.sp, color = U.c.muted)
    val Eyebrow: TextStyle @Composable get() = TextStyle(fontFamily = Alegreya, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, letterSpacing = 0.16.em, color = U.c.rubric)
    val Strong: TextStyle @Composable get() = TextStyle(fontFamily = Alegreya, fontWeight = FontWeight.Bold, fontSize = 16.5.sp, color = U.c.ink)
    val Body: TextStyle @Composable get() = TextStyle(fontFamily = Alegreya, fontSize = 16.sp, lineHeight = 22.sp, color = U.c.ink)
    val Muted: TextStyle @Composable get() = TextStyle(fontFamily = Alegreya, fontSize = 14.sp, lineHeight = 19.sp, color = U.c.muted)
    val Small: TextStyle @Composable get() = TextStyle(fontFamily = Alegreya, fontWeight = FontWeight.Medium, fontSize = 12.5.sp, color = U.c.faint)
    val Number: TextStyle @Composable get() = TextStyle(fontFamily = Cormorant, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = U.c.ink, fontFeatureSettings = "lnum, tnum")
    val Button: TextStyle @Composable get() = TextStyle(fontFamily = Alegreya, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    val Script: TextStyle @Composable get() = TextStyle(fontFamily = Cormorant, fontWeight = FontWeight.Medium, fontStyle = FontStyle.Italic, fontSize = 19.sp, lineHeight = 24.sp, color = U.c.muted)
}

@Composable
fun VigilTheme(content: @Composable () -> Unit) {
    val p = Parchment
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(
            colorScheme = lightColorScheme(
                primary = p.rubric,
                onPrimary = p.onRubric,
                primaryContainer = p.rubricSoft,
                onPrimaryContainer = p.ink,
                secondary = p.lapis,
                secondaryContainer = p.goldSoft,
                onSecondaryContainer = p.ink,
                background = p.bg,
                surface = p.surface,
                surfaceContainer = p.surface,
                surfaceContainerHigh = p.surface,
                surfaceContainerHighest = p.surface2,
                surfaceContainerLow = p.surface2,
                onBackground = p.ink,
                onSurface = p.ink,
                onSurfaceVariant = p.muted,
                outline = p.line,
                outlineVariant = p.line,
                error = p.rubric,
            ),
            typography = MaterialTheme.typography.copy(
                bodyLarge = UType.Body,
                bodyMedium = UType.Body.copy(fontSize = 15.sp),
                labelLarge = UType.Button,
            ),
            content = content,
        )
    }
}
