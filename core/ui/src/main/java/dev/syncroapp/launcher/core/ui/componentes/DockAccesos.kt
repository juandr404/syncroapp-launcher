package dev.syncroapp.launcher.core.ui.componentes

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.syncroapp.launcher.core.ui.tema.Espacio
import dev.syncroapp.launcher.core.ui.tema.TemaLauncher

/** Un acceso del dock: la app y su icono ya rasterizado (null mientras carga). */
data class AccesoDock(
    val clave: String,
    val etiqueta: String,
    val icono: ImageBitmap?,
)

/**
 * Dock inferior: accesos rapidos en circulos de contorno.
 *
 * Es la contraparte de la lista: la lista se lee, el dock se toca sin mirar. Van los gestos de
 * musculo (telefono, camara, mensajes) donde el pulgar llega sin estirarse, y por eso el circulo
 * completo mide 46 dp aunque el icono dibuje 22.
 */
@Composable
fun DockAccesos(
    accesos: List<AccesoDock>,
    onAbrir: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (accesos.isEmpty()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Espacio.m),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        accesos.forEach { acceso ->
            CirculoAcceso(acceso = acceso, onAbrir = { onAbrir(acceso.clave) })
        }
    }
}

@Composable
private fun CirculoAcceso(acceso: AccesoDock, onAbrir: () -> Unit) {
    val colores = TemaLauncher.colores

    val interaccion = remember { MutableInteractionSource() }
    val presionado by interaccion.collectIsPressedAsState()

    Box(
        modifier = Modifier
            .size(DIAMETRO)
            .border(
                width = 1.dp,
                // Un contorno tenue: define el area tactil sin competir con el texto de la lista.
                color = colores.textoPrimario.copy(alpha = if (presionado) 0.5f else 0.28f),
                shape = CircleShape,
            )
            .background(
                color = colores.textoPrimario.copy(alpha = if (presionado) 0.12f else 0.04f),
                shape = CircleShape,
            )
            .clickable(
                interactionSource = interaccion,
                indication = null,
                onClick = onAbrir,
            )
            .semantics { contentDescription = "Abrir ${acceso.etiqueta}" },
        contentAlignment = Alignment.Center,
    ) {
        acceso.icono?.let { icono ->
            Image(
                bitmap = icono,
                contentDescription = null, // el circulo entero ya se anuncia
                modifier = Modifier.size(TAMANO_ICONO),
            )
        }
    }
}

/** 46 dp: por debajo del minimo tactil de 48 seria un boton que se falla al tocar. */
private val DIAMETRO = 46.dp
private val TAMANO_ICONO = 22.dp
