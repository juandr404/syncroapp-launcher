package dev.syncroapp.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.syncroapp.launcher.ajustes.PantallaAjustes
import dev.syncroapp.launcher.cajon.PantallaCajon
import dev.syncroapp.launcher.core.ui.tema.TemaLauncher
import dev.syncroapp.launcher.core.ui.tema.TemaSyncroLauncher
import dev.syncroapp.launcher.inicio.PantallaInicio
import dev.syncroapp.launcher.navegacion.Pantalla

/** Duraciones del sistema de diseno: el movimiento confirma, no entretiene. */
private const val MS_ENTRADA = 220
private const val MS_SALIDA = 180

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
    val esPredeterminado by viewModel.esPredeterminado.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.revisarSiEsPredeterminado()
        onPauseOrDispose { }
    }

    TemaSyncroLauncher(tema = ajustes.tema, blancoMaximo = ajustes.blancoMaximo) {
        // Desde una pantalla interna, atras siempre vuelve al inicio.
        //
        // Estando ya en el inicio depende de si somos el launcher del sistema: si lo somos,
        // atras no hace nada (cerrar la pantalla de inicio dejaria al usuario sin telefono);
        // si no lo somos, se deja pasar al sistema para que cierre la app como cualquier otra,
        // porque si no el usuario queda encerrado sin forma de salir.
        BackHandler(enabled = pantallaActual != Pantalla.INICIO || esPredeterminado) {
            if (pantallaActual != Pantalla.INICIO) onCambiarPantalla(Pantalla.INICIO)
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = TemaLauncher.colores.fondo,
        ) {
            AnimatedContent(
                targetState = pantallaActual,
                transitionSpec = {
                    if (targetState == Pantalla.INICIO) {
                        // Al volver, la pantalla interna se desliza hacia abajo y se desvanece.
                        fadeIn(tween(MS_SALIDA)) togetherWith
                            slideOutVertically(tween(MS_SALIDA)) { alto -> alto / 8 } +
                            fadeOut(tween(MS_SALIDA))
                    } else {
                        // Al entrar, sube desde abajo: el cajon "viene" del gesto que lo abrio.
                        slideInVertically(tween(MS_ENTRADA)) { alto -> alto / 8 } +
                            fadeIn(tween(MS_ENTRADA)) togetherWith fadeOut(tween(MS_SALIDA))
                    }
                },
                label = "transicion-pantallas",
            ) { pantalla ->
                when (pantalla) {
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
}
