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
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.syncroapp.launcher.core.data.modelo.Alineacion
import dev.syncroapp.launcher.core.data.modelo.Densidad
import dev.syncroapp.launcher.core.data.modelo.EstiloIconos
import dev.syncroapp.launcher.core.data.modelo.EstiloReloj
import dev.syncroapp.launcher.core.data.modelo.GrosorTrazo
import dev.syncroapp.launcher.core.data.modelo.TamanoDia
import dev.syncroapp.launcher.core.data.modelo.Tema
import dev.syncroapp.launcher.core.launcherapps.EstadoGestos
import dev.syncroapp.launcher.core.launcherapps.GuardianDeGestos
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
            FilaOpcion("Estilo del reloj", etiquetaEstiloReloj(ajustes.estiloReloj)) {
                dialogoAbierto = DialogoOpciones.ESTILO_RELOJ
            }
        }
        item {
            FilaOpcion("Tamano", etiquetaTamanoDia(ajustes.tamanoDia)) {
                dialogoAbierto = DialogoOpciones.TAMANO_DIA
            }
        }
        item {
            FilaInterruptor(
                titulo = "Mostrar el dia de la semana",
                activo = ajustes.mostrarDiaGigante,
                onCambio = viewModel::cambiarMostrarDiaGigante,
            )
        }
        item {
            FilaInterruptor(
                titulo = "Mostrar la fecha",
                activo = ajustes.mostrarFecha,
                onCambio = viewModel::cambiarMostrarFecha,
            )
        }
        // El grosor solo aplica al contorno del dia gigante.
        if (ajustes.estiloReloj == EstiloReloj.DIA_GIGANTE) {
            item {
                FilaOpcion("Grosor del trazo", etiquetaGrosor(ajustes.grosorTrazo)) {
                    dialogoAbierto = DialogoOpciones.GROSOR
                }
            }
        }
        item {
            FilaInterruptor(
                titulo = "Dia en ingles",
                detalle = "WEDNESDAY en vez de MIERCOLES",
                activo = ajustes.diaEnIngles,
                onCambio = viewModel::cambiarDiaEnIngles,
            )
        }

        // --- Aplicaciones ---
        item { TituloSeccion("Aplicaciones") }
        item {
            FilaOpcion("Iconos en el cajon", etiquetaEstiloIconos(ajustes.estiloIconos)) {
                dialogoAbierto = DialogoOpciones.ICONOS
            }
        }
        if (ajustes.estiloIconos != EstiloIconos.NINGUNO) {
            item {
                FilaInterruptor(
                    titulo = "Iconos tambien en favoritos",
                    detalle = "En el inicio son pocos y elegidos a mano; el texto solo suele bastar",
                    activo = ajustes.iconosEnFavoritos,
                    onCambio = viewModel::cambiarIconosEnFavoritos,
                )
            }
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

        // --- Navegacion del sistema: en MIUI los gestos no pueden funcionar (ver el dialogo) ---
        if (estado.estadoGestos != EstadoGestos.NO_APLICA) {
            item { TituloSeccion("Navegacion del sistema") }
            if (estado.estadoGestos == EstadoGestos.ACTIVOS_PERO_SIN_MANEJADOR) {
                item {
                    FilaAccion(
                        titulo = "Recuperar los botones de navegacion",
                        detalle = "Los gestos estan encendidos pero MIUI no los atiende: " +
                            "el telefono queda sin forma de volver atras. Toque para arreglarlo.",
                        onClick = { viewModel.volverABotones() },
                    )
                }
            }
            item {
                FilaAccion(
                    titulo = "Por que los gestos no funcionan en Xiaomi",
                    detalle = null,
                    onClick = { dialogoAbierto = DialogoOpciones.AYUDA_GESTOS },
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

        DialogoOpciones.ESTILO_RELOJ -> DialogoSeleccion(
            titulo = "Estilo del reloj",
            opciones = EstiloReloj.entries.map { it to etiquetaEstiloReloj(it) },
            seleccionActual = ajustes.estiloReloj,
            onSeleccionar = viewModel::cambiarEstiloReloj,
            onCerrar = { dialogoAbierto = null },
        )

        DialogoOpciones.ICONOS -> DialogoSeleccion(
            titulo = "Iconos en el cajon",
            opciones = EstiloIconos.entries.map { it to etiquetaEstiloIconos(it) },
            seleccionActual = ajustes.estiloIconos,
            onSeleccionar = viewModel::cambiarEstiloIconos,
            onCerrar = { dialogoAbierto = null },
        )

        DialogoOpciones.TAMANO_DIA -> DialogoSeleccion(
            titulo = "Tamano del dia",
            opciones = TamanoDia.entries.map { it to etiquetaTamanoDia(it) },
            seleccionActual = ajustes.tamanoDia,
            onSeleccionar = viewModel::cambiarTamanoDia,
            onCerrar = { dialogoAbierto = null },
        )

        DialogoOpciones.AYUDA_GESTOS -> {
            val colores = TemaLauncher.colores
            val portapapeles = LocalClipboardManager.current
            AlertDialog(
                onDismissRequest = { dialogoAbierto = null },
                containerColor = colores.superficie,
                title = { Text("Gestos en Xiaomi", color = colores.textoPrimario) },
                text = {
                    Text(
                        "En este telefono los gestos de atras, inicio y aplicaciones recientes " +
                            "los implementa el launcher de Xiaomi, no Android. Es el unico que " +
                            "ofrece el servicio del sistema encargado de esos gestos.\n\n" +
                            "Cuando otro launcher pasa a ser el predeterminado, MIUI deja de " +
                            "atenderlos y no pone un reemplazo. Encender los gestos a la fuerza " +
                            "solo esconde los botones sin devolver la navegacion, y el telefono " +
                            "queda sin ninguna forma de salir de una aplicacion.\n\n" +
                            "Por eso esta app ya no los enciende: en Xiaomi la opcion sana es " +
                            "usar los botones de navegacion. En Android puro (Pixel) esta " +
                            "limitacion no existe y los gestos funcionan con cualquier launcher.\n\n" +
                            "Si quedo sin botones y sin gestos, la salida manual es:\n" +
                            "Ajustes de MIUI → Ajustes adicionales → Pantalla completa → " +
                            "Botones de navegacion.",
                        color = colores.textoSecundario,
                    )
                },
                confirmButton = {
                    TextButton(onClick = { dialogoAbierto = null }) {
                        Text("Entendido", color = colores.textoPrimario)
                    }
                },
                dismissButton = {
                    // Solo tiene sentido si el telefono quedo sin navegacion: el permiso habilita
                    // el boton de emergencia que devuelve los botones desde la propia app.
                    if (estado.estadoGestos == EstadoGestos.ACTIVOS_PERO_SIN_MANEJADOR &&
                        !viewModel.tienePermisoDeAjustes()
                    ) {
                        TextButton(onClick = {
                            portapapeles.setText(
                                AnnotatedString(GuardianDeGestos.COMANDO_PARA_OTORGAR),
                            )
                        }) { Text("Copiar comando adb", color = colores.textoSecundario) }
                    }
                },
            )
        }

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

private enum class DialogoOpciones {
    TEMA,
    ALINEACION,
    DENSIDAD,
    GROSOR,
    TAMANO_DIA,
    ICONOS,
    ESTILO_RELOJ,
    AYUDA_GESTOS,
}

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
        // weight(1f) es imprescindible: sin el, un texto largo empuja el interruptor fuera de
        // la pantalla y el detalle se dibuja por debajo de el.
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = Espacio.m),
        ) {
            Text(titulo, style = tipografia.cuerpo.copy(color = colores.textoPrimario))
            if (detalle != null) {
                Text(detalle, style = tipografia.pista.copy(color = colores.textoTerciario))
            }
        }
        Switch(
            checked = activo,
            onCheckedChange = onCambio,
            colors = SwitchDefaults.colors(
                // El launcher es monocromo por identidad: nada de color de acento.
                checkedThumbColor = colores.fondo,
                checkedTrackColor = colores.textoPrimario,
                checkedBorderColor = colores.textoPrimario,
                uncheckedThumbColor = colores.textoTerciario,
                uncheckedTrackColor = colores.fondo,
                uncheckedBorderColor = colores.textoTerciario,
            ),
        )
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

private fun etiquetaEstiloIconos(estilo: EstiloIconos): String = when (estilo) {
    EstiloIconos.NINGUNO -> "Sin iconos"
    EstiloIconos.MONOCROMO -> "Monocromos"
    EstiloIconos.ORIGINALES -> "Originales"
}

private fun etiquetaEstiloReloj(estilo: EstiloReloj): String = when (estilo) {
    EstiloReloj.RELOJ_GRANDE -> "Reloj grande"
    EstiloReloj.DIA_GIGANTE -> "Dia en contorno"
}

private fun etiquetaTamanoDia(tamano: TamanoDia): String = when (tamano) {
    TamanoDia.PEQUENO -> "Pequeno"
    TamanoDia.MEDIANO -> "Mediano"
    TamanoDia.GRANDE -> "Grande"
}
