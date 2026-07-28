package dev.syncroapp.launcher.comun

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.syncroapp.launcher.core.ui.tema.ALTO_TACTIL_MINIMO
import dev.syncroapp.launcher.core.ui.tema.TemaLauncher

/** Una opcion del menu contextual: solo texto, sin iconos. */
data class OpcionMenu(
    val etiqueta: String,
    val accion: () -> Unit,
)

/**
 * Menu contextual de una app (aparece al mantener presionada una fila).
 *
 * Es una lista de texto sobre la superficie del tema: mismas reglas tipograficas que el
 * resto del launcher, sin iconos ni colores de acento.
 */
@Composable
fun MenuContextualApp(
    titulo: String,
    opciones: List<OpcionMenu>,
    onCerrar: () -> Unit,
) {
    val colores = TemaLauncher.colores
    val tipografia = TemaLauncher.tipografia

    AlertDialog(
        onDismissRequest = onCerrar,
        containerColor = colores.superficie,
        title = {
            Text(
                text = titulo,
                style = tipografia.tituloAjustes.copy(color = colores.textoPrimario),
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                opciones.forEach { opcion ->
                    Text(
                        text = opcion.etiqueta,
                        style = tipografia.filaApp.copy(
                            fontSize = 18.sp,
                            color = colores.textoPrimario,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = ALTO_TACTIL_MINIMO)
                            .clickable {
                                opcion.accion()
                                onCerrar()
                            }
                            .padding(vertical = 12.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCerrar) {
                Text(
                    text = "Cerrar",
                    style = tipografia.cuerpo.copy(color = colores.textoSecundario),
                )
            }
        },
    )
}

/**
 * Dialogo para renombrar una app.
 * Dejar el campo vacio restaura el nombre original: no hace falta un boton aparte.
 */
@Composable
fun DialogoRenombrar(
    etiquetaActual: String,
    onConfirmar: (String?) -> Unit,
    onCerrar: () -> Unit,
) {
    val colores = TemaLauncher.colores
    val tipografia = TemaLauncher.tipografia
    var texto by remember { mutableStateOf(etiquetaActual) }

    AlertDialog(
        onDismissRequest = onCerrar,
        containerColor = colores.superficie,
        title = {
            Text(
                text = "Renombrar",
                style = tipografia.tituloAjustes.copy(color = colores.textoPrimario),
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "Deje el campo vacio para usar el nombre original.",
                    style = tipografia.pista.copy(color = colores.textoTerciario),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirmar(texto.trim().ifBlank { null })
                onCerrar()
            }) {
                Text(
                    text = "Guardar",
                    style = tipografia.cuerpo.copy(color = colores.textoPrimario),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) {
                Text(
                    text = "Cancelar",
                    style = tipografia.cuerpo.copy(color = colores.textoSecundario),
                )
            }
        },
    )
}
