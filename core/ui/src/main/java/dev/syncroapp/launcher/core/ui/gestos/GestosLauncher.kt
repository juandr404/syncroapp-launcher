package dev.syncroapp.launcher.core.ui.gestos

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/** Distancia minima para que un arrastre cuente como swipe. */
private val UMBRAL_SWIPE = 60.dp

/**
 * Gestos de la pantalla de inicio.
 *
 * El swipe hacia arriba es estructural (abre el cajon) y por eso no es configurable:
 * si el usuario lo reasigna y olvida cual era, se queda sin forma de llegar a sus apps.
 *
 * Se acumula el desplazamiento y se decide al soltar, en vez de disparar al cruzar el umbral:
 * asi un arrastre dudoso que vuelve al punto de partida no abre nada.
 */
fun Modifier.gestosPantallaInicio(
    onSwipeArriba: () -> Unit,
    onSwipeAbajo: () -> Unit,
    onMantenerPresionado: () -> Unit,
): Modifier = composed {
    val umbralPx = with(LocalDensity.current) { UMBRAL_SWIPE.toPx() }

    this
        .pointerInput(umbralPx) {
            var acumulado = 0f
            detectVerticalDragGestures(
                onDragStart = { acumulado = 0f },
                onDragEnd = {
                    when {
                        acumulado <= -umbralPx -> onSwipeArriba()
                        acumulado >= umbralPx -> onSwipeAbajo()
                    }
                },
                onDragCancel = { acumulado = 0f },
            ) { _, desplazamiento ->
                acumulado += desplazamiento
            }
        }
        .pointerInput(Unit) {
            detectTapGestures(onLongPress = { onMantenerPresionado() })
        }
}
