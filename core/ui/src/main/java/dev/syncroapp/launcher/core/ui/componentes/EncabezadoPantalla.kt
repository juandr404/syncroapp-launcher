package dev.syncroapp.launcher.core.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.syncroapp.launcher.core.ui.tema.ALTO_TACTIL_MINIMO
import dev.syncroapp.launcher.core.ui.tema.Espacio
import dev.syncroapp.launcher.core.ui.tema.TemaLauncher

/**
 * Encabezado de las pantallas internas con una salida siempre visible.
 *
 * El gesto de atras del sistema sigue funcionando, pero no puede ser la unica salida:
 * en las capas de algunos fabricantes se comporta distinto, y una pantalla sin salida
 * visible se siente como una trampa.
 */
@Composable
fun EncabezadoPantalla(
    titulo: String,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = TemaLauncher.colores
    val tipografia = TemaLauncher.tipografia

    val interaccion = remember { MutableInteractionSource() }
    val presionado by interaccion.collectIsPressedAsState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Espacio.margen),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = titulo,
            style = tipografia.tituloAjustes.copy(color = colores.textoPrimario),
        )

        Text(
            text = "Volver",
            style = tipografia.cuerpo.copy(color = colores.textoSecundario),
            modifier = Modifier
                .heightIn(min = ALTO_TACTIL_MINIMO)
                .clickable(
                    interactionSource = interaccion,
                    indication = null,
                    onClick = onVolver,
                )
                .background(if (presionado) colores.presionado else Color.Transparent)
                .padding(horizontal = Espacio.s, vertical = 12.dp)
                .semantics { contentDescription = "Volver al inicio" },
        )
    }
}
