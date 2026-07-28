package dev.syncroapp.launcher.ajustes

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.syncroapp.launcher.core.data.ajustes.RepositorioAjustes
import dev.syncroapp.launcher.core.data.modelo.AjustesLauncher
import dev.syncroapp.launcher.core.data.modelo.Alineacion
import dev.syncroapp.launcher.core.data.modelo.Densidad
import dev.syncroapp.launcher.core.data.modelo.GrosorTrazo
import dev.syncroapp.launcher.core.data.modelo.TamanoDia
import dev.syncroapp.launcher.core.data.modelo.Tema
import dev.syncroapp.launcher.core.launcherapps.AplicacionInstalada
import dev.syncroapp.launcher.core.launcherapps.FuenteApps
import dev.syncroapp.launcher.core.launcherapps.GestorLauncherPredeterminado
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EstadoAjustes(
    val ajustes: AjustesLauncher = AjustesLauncher(),
    /** Apps ocultas ya resueltas, para poder mostrarlas de nuevo desde Ajustes. */
    val appsOcultas: List<AplicacionInstalada> = emptyList(),
    val esPredeterminado: Boolean = true,
)

@HiltViewModel
class AjustesViewModel @Inject constructor(
    private val repositorio: RepositorioAjustes,
    fuenteApps: FuenteApps,
    private val gestorPredeterminado: GestorLauncherPredeterminado,
) : ViewModel() {

    val estado: StateFlow<EstadoAjustes> = combine(
        repositorio.ajustes,
        fuenteApps.apps,
    ) { ajustes, apps ->
        EstadoAjustes(
            ajustes = ajustes,
            appsOcultas = apps.filter { it.claveEstable in ajustes.appsOcultas },
            esPredeterminado = gestorPredeterminado.esPredeterminado(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(TIEMPO_VIVO_MS),
        initialValue = EstadoAjustes(),
    )

    fun intentParaElegirLauncher(): Intent = gestorPredeterminado.intentParaElegir()

    fun intentAjustesDeInicio(): Intent = gestorPredeterminado.intentAjustesDeInicio()

    // --- Pantalla ---
    fun cambiarTema(valor: Tema) = actualizar { it.copy(tema = valor) }
    fun cambiarAlineacion(valor: Alineacion) = actualizar { it.copy(alineacion = valor) }
    fun cambiarDensidad(valor: Densidad) = actualizar { it.copy(densidad = valor) }
    fun cambiarBlancoMaximo(valor: Boolean) = actualizar { it.copy(blancoMaximo = valor) }

    // --- Reloj ---
    fun cambiarFormato24h(valor: Boolean) = actualizar { it.copy(formato24h = valor) }
    fun cambiarMostrarDiaGigante(valor: Boolean) = actualizar { it.copy(mostrarDiaGigante = valor) }
    fun cambiarMostrarFecha(valor: Boolean) = actualizar { it.copy(mostrarFecha = valor) }
    fun cambiarGrosorTrazo(valor: GrosorTrazo) = actualizar { it.copy(grosorTrazo = valor) }
    fun cambiarTamanoDia(valor: TamanoDia) = actualizar { it.copy(tamanoDia = valor) }
    fun cambiarDiaEnIngles(valor: Boolean) = actualizar { it.copy(diaEnIngles = valor) }

    // --- Busqueda ---
    fun cambiarTecladoAutomatico(valor: Boolean) = actualizar { it.copy(tecladoAutomatico = valor) }
    fun cambiarAbrirUnicoResultado(valor: Boolean) = actualizar { it.copy(abrirUnicoResultado = valor) }

    /** Vuelve a mostrar una app oculta. */
    fun mostrarDeNuevo(app: AplicacionInstalada) = viewModelScope.launch {
        repositorio.alternarOculta(app.claveEstable)
    }

    /** Restablece toda la configuracion, conservando los favoritos (perderlos seria brutal). */
    fun restablecer() = actualizar { actual ->
        AjustesLauncher(favoritos = actual.favoritos, onboardingCompletado = true)
    }

    private fun actualizar(transformacion: (AjustesLauncher) -> AjustesLauncher) {
        viewModelScope.launch { repositorio.actualizar(transformacion) }
    }

    private companion object {
        const val TIEMPO_VIVO_MS = 5_000L
    }
}
