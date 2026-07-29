package dev.syncroapp.launcher.cajon

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.syncroapp.launcher.comun.MenuContextualApp
import dev.syncroapp.launcher.comun.OpcionMenu
import dev.syncroapp.launcher.comun.rememberIconoApp
import dev.syncroapp.launcher.core.launcherapps.AplicacionInstalada
import dev.syncroapp.launcher.core.ui.componentes.EncabezadoPantalla
import dev.syncroapp.launcher.core.ui.componentes.FilaApp
import dev.syncroapp.launcher.core.ui.tema.Espacio
import dev.syncroapp.launcher.core.ui.tema.TemaLauncher
import dev.syncroapp.launcher.core.ui.tema.dimensionesDe

/**
 * Cajon de aplicaciones con busqueda.
 *
 * La busqueda es el flujo central: escribir dos letras y presionar Enter debe abrir la app.
 * Por eso el teclado se abre solo y el campo no tiene caja, solo una linea base.
 */
@Composable
fun PantallaCajon(
    onCerrar: () -> Unit,
    viewModel: CajonViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val texto by viewModel.textoBuscado.collectAsStateWithLifecycle()
    val colores = TemaLauncher.colores
    val tipografia = TemaLauncher.tipografia
    val dimensiones = dimensionesDe(estado.ajustes.densidad)

    val enfoqueBusqueda = remember { FocusRequester() }
    var appDelMenu by remember { mutableStateOf<AplicacionInstalada?>(null) }

    // Al entrar al cajon la lista se refresca y el teclado aparece si esta configurado asi.
    LaunchedEffect(Unit) {
        viewModel.refrescar()
        if (estado.ajustes.tecladoAutomatico) {
            runCatching { enfoqueBusqueda.requestFocus() }
        }
    }

    // Un unico resultado: si el usuario lo pidio, se abre sin tener que tocar nada.
    LaunchedEffect(estado.resultados, texto) {
        if (estado.ajustes.abrirUnicoResultado && texto.isNotBlank() && estado.resultados.size == 1) {
            viewModel.abrirApp(estado.resultados.first())
            viewModel.limpiarBusqueda()
            onCerrar()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding(),
    ) {
        EncabezadoPantalla(
            titulo = "Aplicaciones",
            onVolver = {
                viewModel.limpiarBusqueda()
                onCerrar()
            },
            modifier = Modifier.padding(top = Espacio.s),
        )

        CampoBusqueda(
            texto = texto,
            onTextoCambia = viewModel::escribir,
            onBuscar = {
                if (viewModel.abrirPrimerResultado()) {
                    viewModel.limpiarBusqueda()
                    onCerrar()
                }
            },
            enfoque = enfoqueBusqueda,
        )

        if (estado.resultados.isEmpty()) {
            Text(
                text = if (texto.isBlank()) {
                    "No hay aplicaciones para mostrar."
                } else {
                    "No hay resultados para \"$texto\"."
                },
                style = tipografia.cuerpo.copy(color = colores.textoTerciario),
                modifier = Modifier.padding(horizontal = Espacio.margen, vertical = Espacio.l),
            )
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(
                items = estado.resultados,
                key = { it.claveEstable },
            ) { app ->
                FilaApp(
                    etiqueta = app.etiqueta,
                    alineacion = estado.ajustes.alineacion,
                    dimensiones = dimensiones,
                    esPerfilTrabajo = app.esPerfilTrabajo,
                    esFavorito = app.claveEstable in estado.clavesFavoritas,
                    icono = rememberIconoApp(
                        app = app,
                        estilo = estado.ajustes.estiloIconos,
                        tamano = dimensiones.tamanoIcono,
                        cargador = viewModel.cargadorIconos,
                    ),
                    onClick = {
                        viewModel.abrirApp(app)
                        viewModel.limpiarBusqueda()
                        onCerrar()
                    },
                    onLongClick = { appDelMenu = app },
                )
            }
        }
    }

    appDelMenu?.let { app ->
        val esFavorito = app.claveEstable in estado.clavesFavoritas
        MenuContextualApp(
            titulo = app.etiqueta,
            opciones = listOf(
                OpcionMenu(
                    if (esFavorito) "Quitar de favoritos" else "Agregar a favoritos",
                ) { viewModel.alternarFavorito(app) },
                OpcionMenu("Ocultar") { viewModel.ocultar(app) },
                OpcionMenu("Informacion de la app") { viewModel.abrirInfo(app) },
                OpcionMenu("Desinstalar") { viewModel.desinstalar(app) },
            ),
            onCerrar = { appDelMenu = null },
        )
    }
}

/**
 * Campo de busqueda sin caja: solo el texto, el cursor y una linea base de 1 dp.
 * Un contenedor con bordes seria chrome, y aqui el contenido es la interfaz.
 */
@Composable
private fun CampoBusqueda(
    texto: String,
    onTextoCambia: (String) -> Unit,
    onBuscar: () -> Unit,
    enfoque: FocusRequester,
) {
    val colores = TemaLauncher.colores
    val tipografia = TemaLauncher.tipografia

    Column(modifier = Modifier.padding(horizontal = Espacio.margen, vertical = Espacio.m)) {
        BasicTextField(
            value = texto,
            onValueChange = onTextoCambia,
            singleLine = true,
            textStyle = tipografia.filaApp.copy(color = colores.textoPrimario),
            cursorBrush = SolidColor(colores.textoPrimario),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { onBuscar() }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(enfoque),
            decorationBox = { campo ->
                Box {
                    if (texto.isEmpty()) {
                        Text(
                            text = "Buscar",
                            style = tipografia.filaApp.copy(color = colores.textoTerciario),
                        )
                    }
                    campo()
                }
            },
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colores.divisor),
        )
    }
}
