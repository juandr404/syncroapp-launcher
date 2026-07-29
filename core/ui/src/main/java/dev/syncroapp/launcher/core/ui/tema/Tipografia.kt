package dev.syncroapp.launcher.core.ui.tema

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.syncroapp.launcher.core.ui.R

/**
 * Poppins empaquetada en la app.
 *
 * Va como recurso y no como fuente descargable a proposito: las descargables necesitan red y
 * Google Play Services, y este launcher no declara permiso de internet. El precio son ~630 KB
 * de APK; la contrapartida es que la identidad tipografica funciona en un telefono sin datos.
 */
private val Poppins = FontFamily(
    Font(R.font.poppins_extralight, FontWeight.ExtraLight),
    Font(R.font.poppins_light, FontWeight.Light),
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
)

/**
 * Escala tipografica del launcher, con los valores del diseno de referencia.
 *
 * Todo va en sp (nunca dp) para respetar el escalado de fuente del sistema.
 *
 * Los espaciados entre letras se expresan en em (fracciones del tamano de fuente) y no en sp,
 * para que sigan siendo proporcionales cuando el usuario cambia el tamano del texto o el reloj.
 */
@Immutable
data class TipografiaLauncher(
    val familiaDisplay: FontFamily = Poppins,
    val familiaTexto: FontFamily = Poppins,
) {
    /** Hora del bloque compacto (estilo "dia en contorno"). */
    val hora = TextStyle(
        fontFamily = familiaDisplay,
        fontWeight = FontWeight.Normal,
        fontSize = 34.sp,
    )

    val fecha = TextStyle(
        fontFamily = familiaDisplay,
        fontWeight = FontWeight.Light,
        fontSize = 16.sp,
        letterSpacing = 0.18.em,
    )

    /**
     * La hora como protagonista del inicio.
     * Peso 200: a este tamano un trazo muy fino se ve elegante y no llena la pantalla de tinta.
     */
    val horaGrande = TextStyle(
        fontFamily = familiaDisplay,
        fontWeight = FontWeight.ExtraLight,
        letterSpacing = (-0.03).em,
        lineHeight = 0.86.em,
    )

    /** Dia de la semana completo bajo la hora: MIERCOLES muy espaciado. */
    val diaSemana = TextStyle(
        fontFamily = familiaDisplay,
        fontWeight = FontWeight.Light,
        fontSize = 16.sp,
        letterSpacing = 0.34.em,
    )

    /** Fecha con ano bajo el dia: 10 DIC 2026. */
    val fechaLarga = TextStyle(
        fontFamily = familiaDisplay,
        fontWeight = FontWeight.Light,
        fontSize = 12.sp,
        letterSpacing = 0.3.em,
    )

    val filaApp = TextStyle(
        fontFamily = familiaTexto,
        fontWeight = FontWeight.Light,
        fontSize = 18.sp,
        letterSpacing = 0.02.em,
    )

    val tituloAjustes = TextStyle(
        fontFamily = familiaTexto,
        fontWeight = FontWeight.Medium,
        fontSize = 19.sp,
    )

    val cuerpo = TextStyle(
        fontFamily = familiaTexto,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
    )

    val pista = TextStyle(
        fontFamily = familiaTexto,
        fontWeight = FontWeight.Light,
        fontSize = 13.sp,
        letterSpacing = 0.02.em,
    )
}

val LocalTipografia = staticCompositionLocalOf { TipografiaLauncher() }
