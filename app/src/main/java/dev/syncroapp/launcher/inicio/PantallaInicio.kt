package dev.syncroapp.launcher.inicio

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.syncroapp.launcher.comun.DialogoRenombrar
import dev.syncroapp.launcher.comun.MenuContextualApp
import dev.syncroapp.launcher.comun.OpcionMenu
import dev.syncroapp.launcher.comun.rememberIconoApp
import dev.syncroapp.launcher.core.data.modelo.EstiloIconos
import dev.syncroapp.launcher.core.ui.componentes.AccesoDock
import dev.syncroapp.launcher.core.ui.componentes.DockAccesos
import dev.syncroapp.launcher.core.ui.componentes.FilaApp
import dev.syncroapp.launcher.core.ui.componentes.RelojGigante
import dev.syncroapp.launcher.core.ui.gestos.gestosPantallaInicio
import dev.syncroapp.launcher.core.ui.tema.Espacio
import dev.syncroapp.launcher.core.ui.tema.TemaLauncher
import dev.syncroapp.launcher.core.ui.tema.dimensionesDe

/**
 * Pantalla de inicio: reloj + favoritos, y nada mas.
 *
 * Ojo al tocar esta pantalla: NO debe llevar scroll. Un contenedor con scroll consume el
 * arrastre vertical antes de que llegue al detector de gestos del fondo, y eso deja el
 * deslizar-hacia-arriba (la unica forma de llegar al cajon) sin funcionar.
 */
@Composable
fun PantallaInicio(
    onAbrirCajon: () -> Unit,
    onAbrirAjustes: () -> Unit,
    viewModel: InicioViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val dimensiones = dimensionesDe(estado.ajustes.densidad)

    // El usuario pudo cambiar el launcher predeterminado desde fuera; al volver, revisamos.
    LifecycleResumeEffect(Unit) {
        viewModel.revisarSiEsPredeterminado()
        onPauseOrDispose { }
    }

    var appDelMenu by remember { mutableStateOf<FavoritoResuelto?>(null) }
    var appARenombrar by remember { mutableStateOf<FavoritoResuelto?>(null) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .gestosPantallaInicio(
                onSwipeArriba = onAbrirCajon,
                onSwipeAbajo = viewModel::deslizarAbajo,
                onMantenerPresionado = onAbrirAjustes,
            ),
    ) {
        // El bloque del reloj arranca a un 12% de la altura util, con tope de 96 dp.
        val margenSuperior = (maxHeight * 0.12f).coerceAtMost(96.dp)

        // El contenido y el dock van en capas separadas, no como hermanos de una misma columna:
        // si el dock fuera el ultimo hijo de la columna, una lista larga lo comprimiria y sus
        // circulos se dibujarian como elipses aplastadas.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                // Reserva el alto del dock para que la lista nunca se le meta debajo.
                .padding(bottom = if (estado.dock.isEmpty()) 0.dp else ALTO_RESERVADO_DOCK),
        ) {
            Spacer(modifier = Modifier.height(margenSuperior))

            RelojGigante(
                instante = estado.instante,
                formato24h = estado.ajustes.formato24h,
                estiloReloj = estado.ajustes.estiloReloj,
                mostrarDiaGigante = estado.ajustes.mostrarDiaGigante,
                tamanoDia = estado.ajustes.tamanoDia,
                mostrarFecha = estado.ajustes.mostrarFecha,
                grosorTrazo = estado.ajustes.grosorTrazo,
                alineacion = estado.ajustes.alineacion,
                diaEnIngles = estado.ajustes.diaEnIngles,
                onTocarHora = viewModel::tocarHora,
                onTocarFecha = viewModel::tocarFecha,
                modifier = Modifier.padding(horizontal = Espacio.margen),
            )

            Spacer(modifier = Modifier.height(Espacio.relojALista))

            if (estado.sinFavoritosGuardados) {
                PistaSinFavoritos(esPredeterminado = estado.esPredeterminado)
            }

            estado.favoritos.forEach { favorito ->
                FilaApp(
                    etiqueta = favorito.etiquetaVisible,
                    alineacion = estado.ajustes.alineacion,
                    dimensiones = dimensiones,
                    esPerfilTrabajo = favorito.app.esPerfilTrabajo,
                    esFavorito = true,
                    // En el inicio los iconos son siempre monocromos, independiente del estilo
                    // del cajon: la estetica del inicio es de lineas sobre negro.
                    icono = if (estado.ajustes.iconosEnFavoritos) {
                        rememberIconoApp(
                            app = favorito.app,
                            estilo = EstiloIconos.MONOCROMO,
                            tamano = dimensiones.tamanoIcono,
                            cargador = viewModel.cargadorIconos,
                        )
                    } else {
                        null
                    },
                    onClick = { viewModel.abrirApp(favorito.app) },
                    onLongClick = { appDelMenu = favorito },
                )
            }
        }

        DockAccesos(
            accesos = estado.dock.map { acceso ->
                AccesoDock(
                    clave = acceso.app.claveEstable,
                    etiqueta = acceso.etiquetaVisible,
                    icono = rememberIconoApp(
                        app = acceso.app,
                        estilo = EstiloIconos.MONOCROMO,
                        tamano = TAMANO_ICONO_DOCK,
                        cargador = viewModel.cargadorIconos,
                    ),
                )
            },
            onAbrir = { clave ->
                estado.dock.firstOrNull { it.app.claveEstable == clave }
                    ?.let { viewModel.abrirApp(it.app) }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(bottom = Espacio.l),
        )
    }

    appDelMenu?.let { favorito ->
        MenuContextualApp(
            titulo = favorito.etiquetaVisible,
            opciones = listOf(
                OpcionMenu("Renombrar") { appARenombrar = favorito },
                OpcionMenu(
                    if (viewModel.estaEnDock(favorito.app)) "Quitar del dock" else "Agregar al dock",
                ) { viewModel.alternarEnDock(favorito.app) },
                OpcionMenu("Quitar de favoritos") { viewModel.quitarDeFavoritos(favorito.app) },
                OpcionMenu("Informacion de la app") { viewModel.abrirInfo(favorito.app) },
                OpcionMenu("Desinstalar") { viewModel.desinstalar(favorito.app) },
            ),
            onCerrar = { appDelMenu = null },
        )
    }

    appARenombrar?.let { favorito ->
        DialogoRenombrar(
            etiquetaActual = favorito.etiquetaVisible,
            onConfirmar = { alias -> viewModel.renombrar(favorito.app, alias) },
            onCerrar = { appARenombrar = null },
        )
    }
}

/** El icono del dock se rasteriza mas grande que el de la lista: el circulo le da aire. */
private val TAMANO_ICONO_DOCK = 22.dp

/** Diametro del circulo (46) + su separacion del borde (24) + aire respecto a la lista. */
private val ALTO_RESERVADO_DOCK = 86.dp

/**
 * Estado vacio: explica los dos gestos que hacen falta para empezar.
 * Sin ilustraciones y sin sermones; son dos lineas y desaparecen al agregar el primer favorito.
 */
@Composable
private fun PistaSinFavoritos(esPredeterminado: Boolean) {
    val colores = TemaLauncher.colores
    val tipografia = TemaLauncher.tipografia

    Column(modifier = Modifier.padding(horizontal = Espacio.margen)) {
        Text(
            text = "Deslice hacia arriba para ver sus aplicaciones.",
            style = tipografia.cuerpo.copy(color = colores.textoSecundario),
        )
        Text(
            text = "Mantenga presionado el fondo para abrir Ajustes.",
            style = tipografia.pista.copy(color = colores.textoTerciario),
            modifier = Modifier.padding(top = Espacio.s),
        )
        if (!esPredeterminado) {
            Text(
                text = "En Ajustes puede establecerlo como su pantalla de inicio.",
                style = tipografia.pista.copy(color = colores.textoTerciario),
                modifier = Modifier.padding(top = Espacio.s),
            )
        }
    }
}
