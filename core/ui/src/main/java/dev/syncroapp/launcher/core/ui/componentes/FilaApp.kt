package dev.syncroapp.launcher.core.ui.componentes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.syncroapp.launcher.core.data.modelo.Alineacion
import dev.syncroapp.launcher.core.ui.tema.ALTO_TACTIL_MINIMO
import dev.syncroapp.launcher.core.ui.tema.DimensionesLista
import dev.syncroapp.launcher.core.ui.tema.Espacio
import dev.syncroapp.launcher.core.ui.tema.TemaLauncher

/**
 * Una fila de la lista de aplicaciones: solo texto.
 *
 * El area tactil es la fila completa de borde a borde, nunca solo el texto, y nunca baja
 * de 48 dp de alto aunque la densidad compacta dibuje 44.
 *
 * No hay ripple: el feedback es un velo del 8% sobre el fondo, como manda el sistema de diseno
 * (nada de circulos de color expandiendose en una pantalla que quiere ser tinta sobre vidrio).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FilaApp(
    etiqueta: String,
    alineacion: Alineacion,
    dimensiones: DimensionesLista,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    esPerfilTrabajo: Boolean = false,
    esFavorito: Boolean = false,
    /** Icono ya rasterizado; null = fila de solo texto. */
    icono: ImageBitmap? = null,
) {
    val colores = TemaLauncher.colores
    val tipografia = TemaLauncher.tipografia

    val interaccion = remember { MutableInteractionSource() }
    val presionado by interaccion.collectIsPressedAsState()

    val descripcion = buildString {
        append(etiqueta)
        if (esPerfilTrabajo) append(". Perfil de trabajo")
        if (esFavorito) append(". En favoritos")
        append(". Doble toque para abrir. Mantenga presionado para mas opciones.")
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = maxOf(dimensiones.altoFila, ALTO_TACTIL_MINIMO))
            .combinedClickable(
                interactionSource = interaccion,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .background(if (presionado) colores.presionado else Color.Transparent)
            .padding(horizontal = Espacio.margen, vertical = 4.dp)
            .semantics { contentDescription = descripcion },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = when (alineacion) {
            Alineacion.IZQUIERDA -> Arrangement.Start
            Alineacion.CENTRO -> Arrangement.Center
            Alineacion.DERECHA -> Arrangement.End
        },
    ) {
        // Con alineacion a la derecha el icono va del lado de la alineacion, para que el
        // texto siga formando una columna limpia contra el borde.
        if (icono != null && alineacion != Alineacion.DERECHA) {
            Image(
                bitmap = icono,
                contentDescription = null, // la fila entera ya se anuncia como un solo elemento
                modifier = Modifier
                    .size(dimensiones.tamanoIcono)
                    .padding(end = 0.dp),
            )
            Spacer(modifier = Modifier.width(dimensiones.separacionIcono))
        }

        Text(
            text = etiqueta,
            style = tipografia.filaApp.copy(
                fontSize = dimensiones.tamanoTexto,
                color = if (esPerfilTrabajo) colores.textoSecundario else colores.textoPrimario,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        if (icono != null && alineacion == Alineacion.DERECHA) {
            Spacer(modifier = Modifier.width(dimensiones.separacionIcono))
            Image(
                bitmap = icono,
                contentDescription = null,
                modifier = Modifier.size(dimensiones.tamanoIcono),
            )
        }
    }
}
