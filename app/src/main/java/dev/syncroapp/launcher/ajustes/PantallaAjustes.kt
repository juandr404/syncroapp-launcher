package dev.syncroapp.launcher.ajustes

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.syncroapp.launcher.core.data.modelo.Alineacion
import dev.syncroapp.launcher.core.data.modelo.Densidad
import dev.syncroapp.launcher.core.data.modelo.GrosorTrazo
import dev.syncroapp.launcher.core.data.modelo.TamanoDia
import dev.syncroapp.launcher.core.data.modelo.Tema
import dev.syncroapp.launcher.core.ui.componentes.EncabezadoPantalla
import dev.syncroapp.launcher.core.ui.tema.ALTO_TACTIL_MINIMO
import dev.syncroapp.launcher.core.ui.tema.Espacio
import dev.syncroapp.launcher.core.ui.tema.TemaLauncher

/**
 * Configuracion del launcher.
 *
 * Estructura plana de maximo dos niveles y sin iconos en las filas: el texto basta.
 * Cada opcion que existe aqui tuvo que justificar por que no alcanzaba con un buen default.
 */
@Composable
fun PantallaAjustes(
    onCerrar: () -> Unit,
    viewModel: AjustesViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val ajustes = estado.ajustes
    val contexto = LocalContext.current

    val selectorLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { }

    var dialogoAbierto by remember { mutableStateOf<DialogoOpciones?>(null) }
    var confirmarRestablecer by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        item {
            EncabezadoPantalla(
                titulo = "Ajustes",
                onVolver = onCerrar,
                modifier = Modifier.padding(top = Espacio.m, bottom = Espacio.s),
            )
        }

        // --- Pantalla de inicio del sistema ---
        item { TituloSeccion("Pantalla de inicio") }
        item {
            FilaAccion(
                titulo = if (estado.esPredeterminado) {
                    "Es su pantalla de inicio"
                } else {
                    "Establecer como pantalla de inicio"
                },
                detalle = if (estado.esPredeterminado) {
                    "SyncroApp Launcher esta activo"
                } else {
                    "Toque para elegirlo en el sistema"
                },
                onClick = { selectorLauncher.launch(viewModel.intentParaElegirLauncher()) },
            )
        }
        item {
            FilaAccion(
                titulo = "Abrir ajustes de inicio del sistema",
                detalle = "Uselo si el dialogo anterior no aparece",
                onClick = { contexto.startActivity(viewModel.intentAjustesDeInicio()) },
            )
        }

        // --- Pantalla ---
        item { TituloSeccion("Pantalla") }
        item {
            FilaOpcion("Tema", etiquetaTema(ajustes.tema)) {
                dialogoAbierto = DialogoOpciones.TEMA
            }
        }
        item {
            FilaOpcion("Alineacion", etiquetaAlineacion(ajustes.alineacion)) {
                dialogoAbierto = DialogoOpciones.ALINEACION
            }
        }
        item {
            FilaOpcion("Densidad de lista", etiquetaDensidad(ajustes.densidad)) {
                dialogoAbierto = DialogoOpciones.DENSIDAD
            }
        }
        item {
            FilaInterruptor(
                titulo = "Blanco maximo",
                detalle = "Texto en blanco puro; puede dejar rastro en pantallas OLED",
                activo = ajustes.blancoMaximo,
                onCambio = viewModel::cambiarBlancoMaximo,
            )
        }

        // --- Reloj ---
        item { TituloSeccion("Reloj") }
        item {
            FilaInterruptor(
                titulo = "Formato 24 horas",
                activo = ajustes.formato24h,
                onCambio = viewModel::cambiarFormato24h,
            )
        }
        item {
            FilaInterruptor(
                titulo = "Mostrar el dia de la semana",
                activo = ajustes.mostrarDiaGigante,
                onCambio = viewModel::cambiarMostrarDiaGigante,
            )
        }
        if (ajustes.mostrarDiaGigante) {
            item {
                FilaOpcion("Tamano del dia", etiquetaTamanoDia(ajustes.tamanoDia)) {
                    dialogoAbierto = DialogoOpciones.TAMANO_DIA
                }
            }
        }
        item {
            FilaInterruptor(
                titulo = "Mostrar la fecha",
                activo = ajustes.mostrarFecha,
                onCambio = viewModel::cambiarMostrarFecha,
            )
        }
        item {
            FilaOpcion("Grosor del trazo", etiquetaGrosor(ajustes.grosorTrazo)) {
                dialogoAbierto = DialogoOpciones.GROSOR
            }
        }
        item {
            FilaInterruptor(
                titulo = "Dia en ingles",
                detalle = "WED en vez de MIE",
                activo = ajustes.diaEnIngles,
                onCambio = viewModel::cambiarDiaEnIngles,
            )
        }

        // --- Busqueda ---
        item { TituloSeccion("Busqueda") }
        item {
            FilaInterruptor(
                titulo = "Abrir el teclado automaticamente",
                activo = ajustes.tecladoAutomatico,
                onCambio = viewModel::cambiarTecladoAutomatico,
            )
        }
        item {
            FilaInterruptor(
                titulo = "Abrir si queda un solo resultado",
                detalle = "Sin tener que presionar Enter",
                activo = ajustes.abrirUnicoResultado,
                onCambio = viewModel::cambiarAbrirUnicoResultado,
            )
        }

        // --- Apps ocultas ---
        if (estado.appsOcultas.isNotEmpty()) {
            item { TituloSeccion("Aplicaciones ocultas") }
            items(estado.appsOcultas, key = { it.claveEstable }) { app ->
                FilaAccion(
                    titulo = app.etiqueta,
                    detalle = "Toque para volver a mostrarla",
                    onClick = { viewModel.mostrarDeNuevo(app) },
                )
            }
        }

        // --- Acerca de ---
        item { TituloSeccion("Acerca de") }
        item {
            FilaAccion(
                titulo = "SyncroApp Launcher",
                detalle = "Version 0.1.0 - software libre, sin rastreadores, sin acceso a internet",
                onClick = { },
            )
        }
        item {
            FilaAccion(
                titulo = "Restablecer configuracion",
                detalle = "Conserva sus favoritos",
                onClick = { confirmarRestablecer = true },
            )
        }
        item {
            FilaAccion(
                titulo = "Volver al inicio",
                detalle = null,
                onClick = onCerrar,
            )
        }
    }

    when (dialogoAbierto) {
        DialogoOpciones.TEMA -> DialogoSeleccion(
            titulo = "Tema",
            opciones = Tema.entries.map { it to etiquetaTema(it) },
            seleccionActual = ajustes.tema,
            onSeleccionar = viewModel::cambiarTema,
            onCerrar = { dialogoAbierto = null },
        )

        DialogoOpciones.ALINEACION -> DialogoSeleccion(
            titulo = "Alineacion",
            opciones = Alineacion.entries.map { it to etiquetaAlineacion(it) },
            seleccionActual = ajustes.alineacion,
            onSeleccionar = viewModel::cambiarAlineacion,
            onCerrar = { dialogoAbierto = null },
        )

        DialogoOpciones.DENSIDAD -> DialogoSeleccion(
            titulo = "Densidad de lista",
            opciones = Densidad.entries.map { it to etiquetaDensidad(it) },
            seleccionActual = ajustes.densidad,
            onSeleccionar = viewModel::cambiarDensidad,
            onCerrar = { dialogoAbierto = null },
        )

        DialogoOpciones.GROSOR -> DialogoSeleccion(
            titulo = "Grosor del trazo",
            opciones = GrosorTrazo.entries.map { it to etiquetaGrosor(it) },
            seleccionActual = ajustes.grosorTrazo,
            onSeleccionar = viewModel::cambiarGrosorTrazo,
            onCerrar = { dialogoAbierto = null },
        )

        DialogoOpciones.TAMANO_DIA -> DialogoSeleccion(
            titulo = "Tamano del dia",
            opciones = TamanoDia.entries.map { it to etiquetaTamanoDia(it) },
            seleccionActual = ajustes.tamanoDia,
            onSeleccionar = viewModel::cambiarTamanoDia,
            onCerrar = { dialogoAbierto = null },
        )

        null -> Unit
    }

    if (confirmarRestablecer) {
        val colores = TemaLauncher.colores
        AlertDialog(
            onDismissRequest = { confirmarRestablecer = false },
            containerColor = colores.superficie,
            title = { Text("Restablecer configuracion", color = colores.textoPrimario) },
            text = {
                Text(
                    "Todas las opciones vuelven a sus valores originales. Sus favoritos se conservan.",
                    color = colores.textoSecundario,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.restablecer()
                    confirmarRestablecer = false
                }) { Text("Restablecer", color = colores.textoPrimario) }
            },
            dismissButton = {
                TextButton(onClick = { confirmarRestablecer = false }) {
                    Text("Cancelar", color = colores.textoSecundario)
                }
            },
        )
    }
}

private enum class DialogoOpciones { TEMA, ALINEACION, DENSIDAD, GROSOR, TAMANO_DIA }

// --- Componentes de la pantalla de ajustes ---

@Composable
private fun TituloSeccion(texto: String, primera: Boolean = false) {
    val colores = TemaLauncher.colores
    Text(
        text = texto,
        style = TemaLauncher.tipografia.tituloAjustes.copy(color = colores.textoPrimario),
        modifier = Modifier.padding(
            start = Espacio.margen,
            end = Espacio.margen,
            top = if (primera) Espacio.l else Espacio.xl,
            bottom = Espacio.s,
        ),
    )
}

@Composable
private fun FilaOpcion(titulo: String, valor: String, onClick: () -> Unit) {
    val colores = TemaLauncher.colores
    val tipografia = TemaLauncher.tipografia

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ALTO_TACTIL_MINIMO)
            .clickable(onClick = onClick)
            .padding(horizontal = Espacio.margen, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(titulo, style = tipografia.cuerpo.copy(color = colores.textoPrimario))
        Text(valor, style = tipografia.cuerpo.copy(color = colores.textoSecundario))
    }
}

@Composable
private fun FilaInterruptor(
    titulo: String,
    activo: Boolean,
    onCambio: (Boolean) -> Unit,
    detalle: String? = null,
) {
    val colores = TemaLauncher.colores
    val tipografia = TemaLauncher.tipografia

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ALTO_TACTIL_MINIMO)
            .clickable { onCambio(!activo) }
            .padding(horizontal = Espacio.margen, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.padding(end = Espacio.m)) {
            Text(titulo, style = tipografia.cuerpo.copy(color = colores.textoPrimario))
            if (detalle != null) {
                Text(detalle, style = tipografia.pista.copy(color = colores.textoTerciario))
            }
        }
        Switch(checked = activo, onCheckedChange = onCambio)
    }
}

@Composable
private fun FilaAccion(titulo: String, detalle: String?, onClick: () -> Unit) {
    val colores = TemaLauncher.colores
    val tipografia = TemaLauncher.tipografia

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ALTO_TACTIL_MINIMO)
            .clickable(onClick = onClick)
            .padding(horizontal = Espacio.margen, vertical = 12.dp),
    ) {
        Text(titulo, style = tipografia.cuerpo.copy(color = colores.textoPrimario))
        if (detalle != null) {
            Text(detalle, style = tipografia.pista.copy(color = colores.textoTerciario))
        }
    }
}

@Composable
private fun <T> DialogoSeleccion(
    titulo: String,
    opciones: List<Pair<T, String>>,
    seleccionActual: T,
    onSeleccionar: (T) -> Unit,
    onCerrar: () -> Unit,
) {
    val colores = TemaLauncher.colores
    val tipografia = TemaLauncher.tipografia

    AlertDialog(
        onDismissRequest = onCerrar,
        containerColor = colores.superficie,
        title = { Text(titulo, style = tipografia.tituloAjustes.copy(color = colores.textoPrimario)) },
        text = {
            Column {
                opciones.forEach { (valor, etiqueta) ->
                    val seleccionada = valor == seleccionActual
                    Text(
                        text = if (seleccionada) "$etiqueta  ·" else etiqueta,
                        style = tipografia.cuerpo.copy(
                            color = if (seleccionada) colores.textoPrimario else colores.textoSecundario,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = ALTO_TACTIL_MINIMO)
                            .clickable {
                                onSeleccionar(valor)
                                onCerrar()
                            }
                            .padding(vertical = 12.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCerrar) {
                Text("Cerrar", style = tipografia.cuerpo.copy(color = colores.textoSecundario))
            }
        },
    )
}

// --- Etiquetas legibles de los enums ---

private fun etiquetaTema(tema: Tema): String = when (tema) {
    Tema.DARK_PURO -> "Negro puro"
    Tema.DARK_SUAVE -> "Oscuro suave"
    Tema.CLARO -> "Claro"
    Tema.SEGUN_SISTEMA -> "Segun el sistema"
}

private fun etiquetaAlineacion(alineacion: Alineacion): String = when (alineacion) {
    Alineacion.IZQUIERDA -> "Izquierda"
    Alineacion.CENTRO -> "Centro"
    Alineacion.DERECHA -> "Derecha"
}

private fun etiquetaDensidad(densidad: Densidad): String = when (densidad) {
    Densidad.COMPACTA -> "Compacta"
    Densidad.MEDIA -> "Media"
    Densidad.AMPLIA -> "Amplia"
}

private fun etiquetaGrosor(grosor: GrosorTrazo): String = when (grosor) {
    GrosorTrazo.FINO -> "Fino"
    GrosorTrazo.MEDIO -> "Medio"
    GrosorTrazo.GRUESO -> "Grueso"
}

private fun etiquetaTamanoDia(tamano: TamanoDia): String = when (tamano) {
    TamanoDia.PEQUENO -> "Pequeno"
    TamanoDia.MEDIANO -> "Mediano"
    TamanoDia.GRANDE -> "Grande"
}
