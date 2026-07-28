package dev.syncroapp.launcher.core.ui.tema

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import dev.syncroapp.launcher.core.data.modelo.Tema

/**
 * Tema raiz del launcher.
 *
 * No usa Material You dinamico: el launcher es monocromo por identidad. MaterialTheme se
 * configura igualmente porque algunos controles de Ajustes (interruptores) vienen de Material 3
 * y deben heredar la escala de grises en vez de traer su propio color.
 */
@Composable
fun TemaSyncroLauncher(
    tema: Tema,
    blancoMaximo: Boolean,
    content: @Composable () -> Unit,
) {
    val sistemaEnOscuro = isSystemInDarkTheme()

    val colores = when (tema) {
        Tema.DARK_PURO -> coloresOscuros(blancoMaximo, fondoPuro = true)
        Tema.DARK_SUAVE -> coloresOscuros(blancoMaximo, fondoPuro = false)
        Tema.CLARO -> coloresClaros()
        Tema.SEGUN_SISTEMA ->
            if (sistemaEnOscuro) coloresOscuros(blancoMaximo, fondoPuro = true) else coloresClaros()
    }

    val esquemaMaterial = if (colores.esOscuro) {
        darkColorScheme(
            primary = colores.textoPrimario,
            onPrimary = colores.fondo,
            background = colores.fondo,
            onBackground = colores.textoPrimario,
            surface = colores.superficie,
            onSurface = colores.textoPrimario,
            surfaceVariant = colores.superficie,
            onSurfaceVariant = colores.textoSecundario,
            outline = colores.divisor,
        )
    } else {
        lightColorScheme(
            primary = colores.textoPrimario,
            onPrimary = colores.fondo,
            background = colores.fondo,
            onBackground = colores.textoPrimario,
            surface = colores.superficie,
            onSurface = colores.textoPrimario,
            surfaceVariant = colores.superficie,
            onSurfaceVariant = colores.textoSecundario,
            outline = colores.divisor,
        )
    }

    CompositionLocalProvider(
        LocalColores provides colores,
        LocalTipografia provides TipografiaLauncher(),
    ) {
        MaterialTheme(colorScheme = esquemaMaterial, content = content)
    }
}

/** Acceso corto a los tokens del tema desde cualquier composable. */
object TemaLauncher {
    val colores: ColoresLauncher
        @Composable get() = LocalColores.current

    val tipografia: TipografiaLauncher
        @Composable get() = LocalTipografia.current
}
