package dev.syncroapp.launcher.core.ui.tema

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Escala tipografica del launcher.
 *
 * Todo va en sp (nunca dp) para respetar el escalado de fuente del sistema. El dia gigante
 * se auto-ajusta al ancho disponible, por eso aqui solo se define su rango.
 *
 * v0.1 usa la familia por defecto del sistema. Las fuentes empaquetadas (Archivo para el
 * reloj, Inter para las listas) entran junto con el selector de tipografia.
 */
@Immutable
data class TipografiaLauncher(
    val familiaDisplay: FontFamily = FontFamily.SansSerif,
    val familiaTexto: FontFamily = FontFamily.SansSerif,
) {
    /** Rango del dia de la semana en contorno. Se auto-escala dentro de estos limites. */
    val diaGiganteMinSp: Float = 96f
    val diaGiganteMaxSp: Float = 172f

    val hora = TextStyle(
        fontFamily = familiaDisplay,
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
    )

    val fecha = TextStyle(
        fontFamily = familiaDisplay,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 1.6.sp, // ~0.10em: versalitas simuladas
    )

    val filaApp = TextStyle(
        fontFamily = familiaTexto,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
    )

    val tituloAjustes = TextStyle(
        fontFamily = familiaTexto,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
    )

    val cuerpo = TextStyle(
        fontFamily = familiaTexto,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
    )

    val pista = TextStyle(
        fontFamily = familiaTexto,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        letterSpacing = 0.3.sp,
    )
}

val LocalTipografia = staticCompositionLocalOf { TipografiaLauncher() }
