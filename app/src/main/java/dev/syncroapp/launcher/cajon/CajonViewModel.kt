package dev.syncroapp.launcher.cajon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.syncroapp.launcher.core.data.ajustes.RepositorioAjustes
import dev.syncroapp.launcher.core.data.busqueda.PuntuadorBusqueda
import dev.syncroapp.launcher.core.data.modelo.AjustesLauncher
import dev.syncroapp.launcher.core.launcherapps.AplicacionInstalada
import dev.syncroapp.launcher.core.launcherapps.CargadorIconos
import dev.syncroapp.launcher.core.launcherapps.FuenteApps
import dev.syncroapp.launcher.dominio.AccionesApp
import dev.syncroapp.launcher.dominio.apuntaA
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EstadoCajon(
    val resultados: List<AplicacionInstalada> = emptyList(),
    val ajustes: AjustesLauncher = AjustesLauncher(),
    val clavesFavoritas: Set<String> = emptySet(),
)

@OptIn(FlowPreview::class)
@HiltViewModel
class CajonViewModel @Inject constructor(
    private val fuenteApps: FuenteApps,
    repositorio: RepositorioAjustes,
    private val acciones: AccionesApp,
    /** Lo consume la UI para dibujar el icono de cada fila. */
    val cargadorIconos: CargadorIconos,
) : ViewModel() {

    private val consulta = MutableStateFlow("")

    /**
     * Texto tal como lo escribe el usuario, sin retardo.
     * El campo de texto se dibuja con esto; los resultados usan la version con debounce.
     */
    val textoBuscado: StateFlow<String> = consulta.asStateFlow()

    val estado: StateFlow<EstadoCajon> = combine(
        fuenteApps.apps,
        repositorio.ajustes,
        // 50 ms de gracia: evita recalcular la lista entera entre dos teclas rapidas,
        // sin que se sienta lag al escribir.
        consulta.debounce(RETARDO_BUSQUEDA_MS),
    ) { apps, ajustes, textoBuscado ->
        val visibles = apps.filterNot { it.claveEstable in ajustes.appsOcultas }

        EstadoCajon(
            resultados = filtrarYOrdenar(visibles, textoBuscado),
            ajustes = ajustes,
            clavesFavoritas = ajustes.favoritos
                .mapNotNull { guardado -> apps.firstOrNull { guardado.apuntaA(it) }?.claveEstable }
                .toSet(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(TIEMPO_VIVO_MS),
        initialValue = EstadoCajon(),
    )

    /**
     * Con consulta vacia devuelve todo en orden alfabetico (la lista ya viene ordenada).
     * Con consulta, puntua cada etiqueta y descarta lo que no coincide.
     */
    private fun filtrarYOrdenar(
        apps: List<AplicacionInstalada>,
        textoBuscado: String,
    ): List<AplicacionInstalada> {
        val consultaNormalizada = PuntuadorBusqueda.normalizar(textoBuscado)
        if (consultaNormalizada.isEmpty()) return apps

        return apps
            .map { app -> app to PuntuadorBusqueda.puntuar(consultaNormalizada, app.etiquetaNormalizada) }
            .filter { (_, puntaje) -> puntaje != PuntuadorBusqueda.SIN_COINCIDENCIA }
            .sortedByDescending { (_, puntaje) -> puntaje }
            .map { (app, _) -> app }
    }

    fun escribir(texto: String) {
        consulta.value = texto
    }

    fun limpiarBusqueda() {
        consulta.value = ""
    }

    fun abrirApp(app: AplicacionInstalada) {
        acciones.lanzar(app)
    }

    /** Abre el primer resultado (tecla Enter del teclado). Devuelve false si no hay ninguno. */
    fun abrirPrimerResultado(): Boolean {
        val primero = estado.value.resultados.firstOrNull() ?: return false
        acciones.lanzar(primero)
        return true
    }

    fun alternarFavorito(app: AplicacionInstalada) = viewModelScope.launch {
        acciones.alternarFavorito(app, esFavorito = app.claveEstable in estado.value.clavesFavoritas)
    }

    fun ocultar(app: AplicacionInstalada) = viewModelScope.launch {
        acciones.alternarOculta(app)
    }

    fun abrirInfo(app: AplicacionInstalada) = acciones.abrirInfo(app)

    fun desinstalar(app: AplicacionInstalada) = acciones.desinstalar(app)

    fun refrescar() = fuenteApps.refrescar()

    private companion object {
        const val RETARDO_BUSQUEDA_MS = 50L
        const val TIEMPO_VIVO_MS = 5_000L
    }
}
