package dev.syncroapp.launcher.core.ui.tema

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Paleta base. No hay color de acento por diseno: la jerarquia se construye solo con
 * tamano, peso y trazo (relleno vs. contorno).
 */
object Paleta {
    val NegroTinta = Color(0xFF000000) // fondo OLED puro: el pixel apagado no consume
    val NegroNoche = Color(0xFF101114) // dark suave, para paneles LCD
    val BlancoPapel = Color(0xFFF6F6F3) // blanco calido: menos deslumbrante que #FFF

    val Gris900 = Color(0xFF191A1D)
    val Gris700 = Color(0xFF3A3C40)
    val Gris500 = Color(0xFF7D8085)
    val Gris300 = Color(0xFFB9BBBE)
    val Gris100 = Color(0xFFE8E8E6)

    val BlancoPuro = Color(0xFFFFFFFF)
    val BlancoSuave = Color(0xFFEDEEEF) // default: evita el "halo" del blanco puro en OLED
    val NegroTexto = Color(0xFF141518)
}

/**
 * Tokens semanticos del tema. Los componentes usan estos nombres, nunca colores literales:
 * cambiar de tema es cambiar este objeto, no tocar los componentes.
 */
@Immutable
data class ColoresLauncher(
    val fondo: Color,
    val superficie: Color,
    val textoPrimario: Color,
    val textoSecundario: Color,
    val textoTerciario: Color,
    val divisor: Color,
    val presionado: Color,
    val esOscuro: Boolean,
)

val LocalColores = staticCompositionLocalOf {
    coloresOscuros(blancoMaximo = false, fondoPuro = true)
}

/** Tema oscuro. [fondoPuro] = negro #000000 (insignia); si no, gris carbon. */
fun coloresOscuros(blancoMaximo: Boolean, fondoPuro: Boolean): ColoresLauncher {
    val primario = if (blancoMaximo) Paleta.BlancoPuro else Paleta.BlancoSuave
    return ColoresLauncher(
        fondo = if (fondoPuro) Paleta.NegroTinta else Paleta.NegroNoche,
        superficie = Paleta.Gris900,
        textoPrimario = primario,
        textoSecundario = Paleta.Gris300,
        textoTerciario = Paleta.Gris500,
        divisor = Paleta.Gris700,
        presionado = primario.copy(alpha = 0.08f),
        esOscuro = true,
    )
}

/** Tema claro: misma estructura, tinta invertida. El dia gigante sigue en contorno. */
fun coloresClaros(): ColoresLauncher = ColoresLauncher(
    fondo = Paleta.BlancoPapel,
    superficie = Paleta.BlancoPuro,
    textoPrimario = Paleta.NegroTexto,
    textoSecundario = Paleta.Gris700,
    textoTerciario = Paleta.Gris500,
    divisor = Paleta.Gris100,
    presionado = Paleta.NegroTexto.copy(alpha = 0.08f),
    esOscuro = false,
)
