package dev.syncroapp.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.syncroapp.launcher.ajustes.PantallaAjustes
import dev.syncroapp.launcher.cajon.PantallaCajon
import dev.syncroapp.launcher.inicio.PantallaInicio
import dev.syncroapp.launcher.navegacion.Pantalla
import dev.syncroapp.launcher.core.ui.tema.TemaLauncher
import dev.syncroapp.launcher.core.ui.tema.TemaSyncroLauncher

/**
 * Raiz de la interfaz: aplica el tema segun la configuracion y decide que pantalla mostrar.
 */
@Composable
fun RaizLauncher(
    pantallaActual: Pantalla,
    onCambiarPantalla: (Pantalla) -> Unit,
) {
    val viewModel: RaizViewModel = hiltViewModel()
    val ajustes by viewModel.ajustes.collectAsStateWithLifecycle()

    TemaSyncroLauncher(tema = ajustes.tema, blancoMaximo = ajustes.blancoMaximo) {
        // Back siempre interceptado: nunca debe cerrar la actividad del launcher.
        // Desde una pantalla interna vuelve a Inicio; desde Inicio no hace nada.
        BackHandler(enabled = true) {
            if (pantallaActual != Pantalla.INICIO) onCambiarPantalla(Pantalla.INICIO)
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = TemaLauncher.colores.fondo,
        ) {
            when (pantallaActual) {
                Pantalla.INICIO -> PantallaInicio(
                    onAbrirCajon = { onCambiarPantalla(Pantalla.CAJON) },
                    onAbrirAjustes = { onCambiarPantalla(Pantalla.AJUSTES) },
                )

                Pantalla.CAJON -> PantallaCajon(
                    onCerrar = { onCambiarPantalla(Pantalla.INICIO) },
                )

                Pantalla.AJUSTES -> PantallaAjustes(
                    onCerrar = { onCambiarPantalla(Pantalla.INICIO) },
                )
            }
        }
    }
}
