package dev.syncroapp.launcher.core.ui.tema

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.syncroapp.launcher.core.data.modelo.Densidad
import dev.syncroapp.launcher.core.data.modelo.GrosorTrazo

/** Escala de espaciado. Unidad base 4 dp. */
object Espacio {
    val xs = 4.dp
    val s = 8.dp
    val m = 16.dp
    val l = 24.dp
    val xl = 32.dp
    val xxl = 48.dp

    /** Margen lateral del contenido. */
    val margen = 24.dp

    /** Separacion entre el bloque del reloj y la lista de favoritos. */
    val relojALista = 56.dp
}

/** Medidas de las filas de apps segun la densidad elegida. */
@Immutable
data class DimensionesLista(
    val altoFila: Dp,
    val tamanoTexto: TextUnit,
    val separacionIcono: Dp,
)

fun dimensionesDe(densidad: Densidad): DimensionesLista = when (densidad) {
    Densidad.COMPACTA -> DimensionesLista(44.dp, 19.sp, 12.dp)
    Densidad.MEDIA -> DimensionesLista(52.dp, 22.sp, 16.dp)
    Densidad.AMPLIA -> DimensionesLista(64.dp, 25.sp, 16.dp)
}

/**
 * Grosor del contorno del dia gigante.
 * Va en dp y no en sp: es una constante visual, no debe crecer con el escalado de fuente.
 */
fun anchoTrazoDe(grosor: GrosorTrazo): Dp = when (grosor) {
    GrosorTrazo.FINO -> 2.dp
    GrosorTrazo.MEDIO -> 3.dp
    GrosorTrazo.GRUESO -> 4.5.dp
}

/** Alto minimo de un objetivo tactil segun las guias de accesibilidad. */
val ALTO_TACTIL_MINIMO = 48.dp
