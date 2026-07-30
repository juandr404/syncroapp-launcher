package dev.syncroapp.launcher.cajon

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Velocity
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
@OptIn(ExperimentalFoundationApi::class)
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

    val cerrarCajon = {
        viewModel.limpiarBusqueda()
        onCerrar()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
            .nestedScroll(recordarCierrePorArrastre(alCerrar = cerrarCajon)),
    ) {
        EncabezadoPantalla(
            titulo = "Aplicaciones",
            onVolver = cerrarCajon,
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

        // Sin el efecto de estiramiento del borde.
        //
        // No es una decision estetica: ese efecto CONSUME el arrastre que la lista ya no puede
        // usar, y por eso el sobrante nunca llegaba al detector de cierre. Quitarlo hace que el
        // gesto de bajar para cerrar funcione, y de paso encaja con el sistema de diseno, que
        // descarta los rebotes elasticos.
        CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
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
                            cerrarCajon()
                        },
                        onLongClick = { appDelMenu = app },
                    )
                }
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
 * Cierra el cajon al arrastrar hacia abajo, como inverso del gesto que lo abre.
 *
 * Se implementa con nested scroll y no con un detector de arrastre propio por una razon
 * concreta: `onPostScroll` solo recibe el desplazamiento que la lista NO consumio. Con la lista
 * a media altura, el arrastre hacia abajo se lo lleva la lista para subir, y aqui no llega
 * nada; el cierre solo se dispara cuando la lista ya esta arriba y sobra movimiento. Ese
 * "primero subo, luego cierro" es lo que hace que el gesto no se sienta torpe.
 */
@Composable
private fun recordarCierrePorArrastre(alCerrar: () -> Unit): NestedScrollConnection {
    val umbral = with(LocalDensity.current) { UMBRAL_CIERRE.toPx() }

    return remember(alCerrar, umbral) {
        object : NestedScrollConnection {
            /** Suma del arrastre hacia abajo sobrante desde que empezo el gesto. */
            private var acumulado = 0f

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                when {
                    available.y > 0f -> acumulado += available.y
                    // Cualquier movimiento hacia arriba cancela: el usuario cambio de idea.
                    available.y < 0f -> acumulado = 0f
                }
                return Offset.Zero
            }

            /**
             * El cierre se dispara aqui, al terminar el gesto, y no en pleno arrastre.
             *
             * Cerrando a media caricia el resto del arrastre caia sobre la pantalla de inicio,
             * que interpreta el deslizar hacia abajo como "abrir notificaciones": un solo gesto
             * terminaba cerrando el cajon Y bajando el panel del sistema. Esperar a que el dedo
             * se levante lo evita, y de paso deja cambiar de idea a mitad del gesto.
             */
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                val cerrar = acumulado >= umbral
                acumulado = 0f
                if (cerrar) alCerrar()
                return Velocity.Zero
            }
        }
    }
}

/** ~56 dp de arrastre sobrante. Menos que esto se dispararia con un rebote de la lista. */
private val UMBRAL_CIERRE = 56.dp

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
                .focusRequester(enfoque)
                // La accion "Ir" del teclado en pantalla cubre el caso normal, pero un teclado
                // fisico o Bluetooth manda una tecla Enter cruda que Compose no traduce a
                // ImeAction. Sin esto, Enter no hace nada con un teclado conectado.
                .onPreviewKeyEvent { evento ->
                    val esEnter = evento.key == Key.Enter || evento.key == Key.NumPadEnter
                    if (esEnter && evento.type == KeyEventType.KeyUp) {
                        onBuscar()
                        true
                    } else {
                        false
                    }
                },
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
