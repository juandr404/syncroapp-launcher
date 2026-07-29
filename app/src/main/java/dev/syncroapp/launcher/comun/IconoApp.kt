package dev.syncroapp.launcher.comun

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import dev.syncroapp.launcher.core.data.modelo.EstiloIconos
import dev.syncroapp.launcher.core.launcherapps.AplicacionInstalada
import dev.syncroapp.launcher.core.launcherapps.CargadorIconos
import dev.syncroapp.launcher.core.launcherapps.EstiloIcono
import dev.syncroapp.launcher.core.ui.tema.TemaLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Carga el icono de una app fuera del hilo principal y lo entrega cuando esta listo.
 *
 * Devuelve null mientras carga, y la fila se dibuja como texto solo. Eso es intencional: es
 * preferible ver la lista completa de inmediato y que los iconos aparezcan un instante despues,
 * a que la lista se quede en blanco esperando a rasterizar doscientos iconos.
 */
@Composable
fun rememberIconoApp(
    app: AplicacionInstalada,
    estilo: EstiloIconos,
    tamano: Dp,
    cargador: CargadorIconos,
): ImageBitmap? {
    val tamanoPx = with(LocalDensity.current) { tamano.roundToPx() }
    val colorTinte = TemaLauncher.colores.textoPrimario.toArgb()

    val icono by produceState<ImageBitmap?>(
        initialValue = null,
        app.claveEstable,
        estilo,
        tamanoPx,
        colorTinte,
    ) {
        value = when (estilo) {
            EstiloIconos.NINGUNO -> null
            else -> withContext(Dispatchers.Default) {
                val estiloInterno = if (estilo == EstiloIconos.MONOCROMO) {
                    EstiloIcono.MONOCROMO
                } else {
                    EstiloIcono.ORIGINAL
                }
                cargador.cargar(app, estiloInterno, tamanoPx, colorTinte)?.asImageBitmap()
            }
        }
    }

    return icono
}
