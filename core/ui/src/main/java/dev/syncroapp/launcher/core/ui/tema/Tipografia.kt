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
    val hora = TextStyle(
        fontFamily = familiaDisplay,
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
    )

    /**
     * La hora como protagonista del inicio (estilo "reloj grande").
     * Peso fino: a este tamano un trazo delgado se ve elegante y no oscurece la pantalla.
     */
    val horaGrande = TextStyle(
        fontFamily = familiaDisplay,
        fontWeight = FontWeight.Light,
        letterSpacing = (-2).sp, // los dos puntos y los digitos respiran menos a este tamano
    )

    /** Dia de la semana completo bajo la hora grande: MIERCOLES con aire entre letras. */
    val diaSemana = TextStyle(
        fontFamily = familiaDisplay,
        fontWeight = FontWeight.Normal,
        fontSize = 26.sp,
        letterSpacing = 7.sp, // ~0.27em, el espaciado amplio del diseno de referencia
    )

    /** Fecha con ano bajo el dia: 10 DIC 2026. */
    val fechaLarga = TextStyle(
        fontFamily = familiaDisplay,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        letterSpacing = 4.sp,
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
