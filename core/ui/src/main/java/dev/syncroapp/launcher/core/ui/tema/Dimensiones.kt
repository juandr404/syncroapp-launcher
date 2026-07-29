package dev.syncroapp.launcher.core.ui.tema

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.syncroapp.launcher.core.data.modelo.Densidad
import dev.syncroapp.launcher.core.data.modelo.GrosorTrazo
import dev.syncroapp.launcher.core.data.modelo.TamanoDia

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
    val tamanoIcono: Dp,
    val separacionIcono: Dp,
)

fun dimensionesDe(densidad: Densidad): DimensionesLista = when (densidad) {
    Densidad.COMPACTA -> DimensionesLista(44.dp, 19.sp, 22.dp, 14.dp)
    Densidad.MEDIA -> DimensionesLista(52.dp, 22.sp, 26.dp, 16.dp)
    Densidad.AMPLIA -> DimensionesLista(64.dp, 25.sp, 30.dp, 18.dp)
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

/**
 * Rango de tamano del dia de la semana, en sp.
 *
 * El texto se auto-escala hasta llenar el ancho disponible sin pasarse del maximo, asi que
 * estos limites son lo que separa "un detalle discreto" de "el dia ocupa media pantalla".
 */
fun rangoTamanoDia(tamano: TamanoDia): ClosedFloatingPointRange<Float> = when (tamano) {
    TamanoDia.PEQUENO -> 40f..64f
    TamanoDia.MEDIANO -> 64f..104f
    TamanoDia.GRANDE -> 96f..172f
}

/** El trazo se afina cuando la letra es chica: un contorno de 4.5 dp en 40 sp la rellena. */
fun anchoTrazoDe(grosor: GrosorTrazo, tamano: TamanoDia): Dp = when (tamano) {
    TamanoDia.PEQUENO -> anchoTrazoDe(grosor) * 0.55f
    TamanoDia.MEDIANO -> anchoTrazoDe(grosor) * 0.8f
    TamanoDia.GRANDE -> anchoTrazoDe(grosor)
}

/** Tamano de la hora cuando es la protagonista del inicio (estilo "reloj grande"). */
fun tamanoHoraGrande(tamano: TamanoDia): TextUnit = when (tamano) {
    TamanoDia.PEQUENO -> 64.sp
    TamanoDia.MEDIANO -> 88.sp
    TamanoDia.GRANDE -> 108.sp
}

/** Alto minimo de un objetivo tactil segun las guias de accesibilidad. */
val ALTO_TACTIL_MINIMO = 48.dp
