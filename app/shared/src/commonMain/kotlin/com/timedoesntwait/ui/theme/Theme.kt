package com.timedoesntwait.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timedoesntwait.domain.AppLanguage
import com.timedoesntwait.resources.Res
import com.timedoesntwait.resources.galmuri11
import com.timedoesntwait.resources.galmuri11_bold
import com.timedoesntwait.resources.space_grotesk_bold
import com.timedoesntwait.resources.space_mono_bold
import com.timedoesntwait.resources.space_mono_regular
import org.jetbrains.compose.resources.Font

/** Halftone Cobalt 1-Bit design tokens (see design doc, "Localization & visual design"). */
object Cobalt {
    val Ink = Color(0xFF1B3693)
    val DeepInk = Color(0xFF001F74)
    val Background = Color(0xFFFBF8FF)
    val Card = Color(0xFFFFFFFF)
    val Danger = Color(0xFFBA1A1A)
    val InkFaint = Color(0x331B3693)

    val Border = 2.dp
    val Shadow = 4.dp
    val ShadowSmall = 3.dp
    val Press = 2.dp
    val Dot = 1.dp
    val DotSpacing = 4.dp
    val MinTouch = 48.dp
}

@Immutable
data class CobaltType(
    val display: TextStyle,
    val title: TextStyle,
    val label: TextStyle,
    val body: TextStyle,
    val small: TextStyle,
    /** What the user types: always Galmuri so Hangul renders in the pixel style. */
    val user: TextStyle,
    val userFont: FontFamily,
)

val LocalCobaltType = staticCompositionLocalOf<CobaltType> { error("CobaltTheme not set") }
val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.EN }

@Composable
fun CobaltTheme(language: AppLanguage, content: @Composable () -> Unit) {
    val galmuri = FontFamily(
        Font(Res.font.galmuri11, FontWeight.Normal),
        Font(Res.font.galmuri11_bold, FontWeight.Bold),
    )
    val heading = when (language) {
        AppLanguage.EN -> FontFamily(Font(Res.font.space_grotesk_bold, FontWeight.Bold))
        AppLanguage.KO -> galmuri
    }
    val mono = when (language) {
        AppLanguage.EN -> FontFamily(
            Font(Res.font.space_mono_regular, FontWeight.Normal),
            Font(Res.font.space_mono_bold, FontWeight.Bold),
        )
        AppLanguage.KO -> galmuri
    }
    val type = CobaltType(
        display = TextStyle(fontFamily = heading, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 30.sp, color = Cobalt.Ink),
        title = TextStyle(fontFamily = heading, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 22.sp, color = Cobalt.Ink),
        label = TextStyle(fontFamily = mono, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp, color = Cobalt.Ink),
        body = TextStyle(fontFamily = mono, fontSize = 14.sp, lineHeight = 20.sp, color = Cobalt.Ink),
        small = TextStyle(fontFamily = mono, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.5.sp, color = Cobalt.Ink),
        user = TextStyle(fontFamily = galmuri, fontSize = 15.sp, lineHeight = 21.sp, color = Cobalt.Ink),
        userFont = galmuri,
    )
    CompositionLocalProvider(
        LocalCobaltType provides type,
        LocalAppLanguage provides language,
        content = content,
    )
}

object CobaltTheme {
    val type: CobaltType @Composable get() = LocalCobaltType.current
    val language: AppLanguage @Composable get() = LocalAppLanguage.current
}
