package dev.syncroapp.launcher.inicio

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.syncroapp.launcher.comun.DialogoRenombrar
import dev.syncroapp.launcher.comun.MenuContextualApp
import dev.syncroapp.launcher.comun.OpcionMenu
import dev.syncroapp.launcher.core.ui.componentes.FilaApp
import dev.syncroapp.launcher.core.ui.componentes.RelojGigante
import dev.syncroapp.launcher.core.ui.gestos.gestosPantallaInicio
import dev.syncroapp.launcher.core.ui.tema.Espacio
import dev.syncroapp.launcher.core.ui.tema.TemaLauncher
import dev.syncroapp.launcher.core.ui.tema.dimensionesDe

/**
 * Pantalla de inicio: reloj + favoritos, y nada mas.
 *
 * El espacio vacio debajo de la lista no se rellena: es parte del diseno.
 */
@Composable
fun PantallaInicio(
    onAbrirCajon: () -> Unit,
    onAbrirAjustes: () -> Unit,
    viewModel: InicioViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val colores = TemaLauncher.colores
    val tipografia = TemaLauncher.tipografia
    val contexto = LocalContext.current
    val dimensiones = dimensionesDe(estado.ajustes.densidad)

    // El usuario pudo cambiar el launcher predeterminado desde fuera; al volver, revisamos.
    LifecycleResumeEffect(Unit) {
        viewModel.revisarSiEsPredeterminado()
        onPauseOrDispose { }
    }

    val selectorLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { viewModel.revisarSiEsPredeterminado() }

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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(margenSuperior))

            RelojGigante(
                instante = estado.instante,
                formato24h = estado.ajustes.formato24h,
                mostrarDiaGigante = estado.ajustes.mostrarDiaGigante,
                mostrarFecha = estado.ajustes.mostrarFecha,
                grosorTrazo = estado.ajustes.grosorTrazo,
                alineacion = estado.ajustes.alineacion,
                diaEnIngles = estado.ajustes.diaEnIngles,
                onTocarHora = viewModel::tocarHora,
                onTocarFecha = viewModel::tocarFecha,
                modifier = Modifier.padding(horizontal = Espacio.margen),
            )

            Spacer(modifier = Modifier.height(Espacio.relojALista))

            if (estado.favoritos.isEmpty() && !estado.cargando) {
                PistaSinFavoritos()
            }

            estado.favoritos.forEach { favorito ->
                FilaApp(
                    etiqueta = favorito.etiquetaVisible,
                    alineacion = estado.ajustes.alineacion,
                    dimensiones = dimensiones,
                    esPerfilTrabajo = favorito.app.esPerfilTrabajo,
                    esFavorito = true,
                    onClick = { viewModel.abrirApp(favorito.app) },
                    onLongClick = { appDelMenu = favorito },
                )
            }

            Spacer(modifier = Modifier.height(Espacio.xxl))
        }

        if (!estado.esPredeterminado) {
            AvisoNoPredeterminado(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(Espacio.margen),
                onEstablecer = { selectorLauncher.launch(viewModel.intentParaElegirLauncher()) },
                onAbrirAjustesDeInicio = { contexto.startActivity(viewModel.intentAjustesDeInicio()) },
            )
        }
    }

    appDelMenu?.let { favorito ->
        MenuContextualApp(
            titulo = favorito.etiquetaVisible,
            opciones = listOf(
                OpcionMenu("Renombrar") { appARenombrar = favorito },
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

/** Estado vacio: dice como llegar a las apps sin sermonear ni ilustrar nada. */
@Composable
private fun PistaSinFavoritos() {
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
    }
}

/**
 * Aviso de que el launcher no es el predeterminado.
 *
 * Ofrece siempre las dos vias: el dialogo del sistema y la pantalla de ajustes de inicio,
 * porque en algunas capas (MIUI entre ellas) el dialogo puede no aparecer.
 */
@Composable
private fun AvisoNoPredeterminado(
    onEstablecer: () -> Unit,
    onAbrirAjustesDeInicio: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = TemaLauncher.colores
    val tipografia = TemaLauncher.tipografia

    Box(modifier = modifier.fillMaxWidth()) {
        Column {
            Text(
                text = "Establecer como pantalla de inicio",
                style = tipografia.cuerpo.copy(color = colores.textoPrimario),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onEstablecer)
                    .padding(vertical = 12.dp),
            )
            Text(
                text = "Si no aparece el dialogo, abra los ajustes de inicio del sistema.",
                style = tipografia.pista.copy(color = colores.textoTerciario),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onAbrirAjustesDeInicio)
                    .padding(bottom = 12.dp),
            )
        }
    }
}
