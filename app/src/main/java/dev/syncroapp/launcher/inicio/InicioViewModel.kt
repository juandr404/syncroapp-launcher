package dev.syncroapp.launcher.inicio

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.syncroapp.launcher.core.data.ajustes.RepositorioAjustes
import dev.syncroapp.launcher.core.data.modelo.AjustesLauncher
import dev.syncroapp.launcher.core.data.reloj.RelojDelSistema
import dev.syncroapp.launcher.core.launcherapps.AccionesSistema
import dev.syncroapp.launcher.core.launcherapps.AplicacionInstalada
import dev.syncroapp.launcher.core.launcherapps.FuenteApps
import dev.syncroapp.launcher.core.launcherapps.GestorLauncherPredeterminado
import dev.syncroapp.launcher.dominio.AccionesApp
import dev.syncroapp.launcher.dominio.apuntaA
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

/** Un favorito ya resuelto contra la lista real de apps instaladas. */
data class FavoritoResuelto(
    val app: AplicacionInstalada,
    /** Alias del usuario si lo hay; si no, la etiqueta original de la app. */
    val etiquetaVisible: String,
)

data class EstadoInicio(
    val instante: LocalDateTime = LocalDateTime.now(),
    val favoritos: List<FavoritoResuelto> = emptyList(),
    val ajustes: AjustesLauncher = AjustesLauncher(),
    val esPredeterminado: Boolean = true,
    /**
     * No hay ningun favorito guardado en la configuracion.
     *
     * Es distinto de `favoritos.isEmpty()`: esa lista tambien esta vacia mientras la lista de
     * aplicaciones del sistema todavia se esta leyendo. Esta bandera se sabe de inmediato, y
     * por eso es la que decide si mostrar las instrucciones iniciales sin hacer esperar a nadie.
     */
    val sinFavoritosGuardados: Boolean = false,
)

@HiltViewModel
class InicioViewModel @Inject constructor(
    reloj: RelojDelSistema,
    private val fuenteApps: FuenteApps,
    private val repositorio: RepositorioAjustes,
    private val acciones: AccionesApp,
    private val accionesSistema: AccionesSistema,
    private val gestorPredeterminado: GestorLauncherPredeterminado,
) : ViewModel() {

    private val esPredeterminado = MutableStateFlow(gestorPredeterminado.esPredeterminado())

    /**
     * El estado se DERIVA de las fuentes reactivas; no se muta a mano en ningun lado.
     * Si se instala una app, cambia un ajuste o pasa un minuto, esto se recalcula solo.
     */
    val estado: StateFlow<EstadoInicio> = combine(
        reloj.instante,
        fuenteApps.apps,
        repositorio.ajustes,
        esPredeterminado,
    ) { instante, apps, ajustes, predeterminado ->
        EstadoInicio(
            instante = instante,
            favoritos = resolverFavoritos(ajustes, apps),
            ajustes = ajustes,
            esPredeterminado = predeterminado,
            sinFavoritosGuardados = ajustes.favoritos.isEmpty(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(TIEMPO_VIVO_MS),
        initialValue = EstadoInicio(),
    )

    /**
     * Cruza los favoritos guardados con las apps que existen de verdad.
     *
     * Los favoritos que apuntan a apps desinstaladas simplemente no se muestran: la lista se
     * autorrepara sola en vez de dejar una entrada muerta que crashea al tocarla.
     */
    private fun resolverFavoritos(
        ajustes: AjustesLauncher,
        apps: List<AplicacionInstalada>,
    ): List<FavoritoResuelto> = ajustes.favoritos.mapNotNull { guardado ->
        apps.firstOrNull { guardado.apuntaA(it) }?.let { app ->
            FavoritoResuelto(app = app, etiquetaVisible = guardado.alias ?: app.etiqueta)
        }
    }

    /** Se llama al volver al launcher: el usuario pudo cambiar el launcher predeterminado afuera. */
    fun revisarSiEsPredeterminado() {
        esPredeterminado.value = gestorPredeterminado.esPredeterminado()
    }

    fun intentParaElegirLauncher(): Intent = gestorPredeterminado.intentParaElegir()

    fun intentAjustesDeInicio(): Intent = gestorPredeterminado.intentAjustesDeInicio()

    fun abrirApp(app: AplicacionInstalada) {
        acciones.lanzar(app)
    }

    fun quitarDeFavoritos(app: AplicacionInstalada) = viewModelScope.launch {
        acciones.alternarFavorito(app, esFavorito = true)
    }

    fun renombrar(app: AplicacionInstalada, alias: String?) = viewModelScope.launch {
        acciones.renombrar(app, alias)
    }

    fun abrirInfo(app: AplicacionInstalada) = acciones.abrirInfo(app)

    fun desinstalar(app: AplicacionInstalada) = acciones.desinstalar(app)

    fun tocarHora() {
        accionesSistema.abrirReloj()
    }

    fun tocarFecha() {
        accionesSistema.abrirCalendario()
    }

    fun deslizarAbajo() {
        accionesSistema.expandirPanelNotificaciones()
    }

    private companion object {
        const val TIEMPO_VIVO_MS = 5_000L
    }
}
