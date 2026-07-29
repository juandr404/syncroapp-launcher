package dev.syncroapp.launcher.dominio

import dev.syncroapp.launcher.core.data.ajustes.RepositorioAjustes
import dev.syncroapp.launcher.core.data.modelo.FavoritoGuardado
import dev.syncroapp.launcher.core.launcherapps.AplicacionInstalada
import dev.syncroapp.launcher.core.launcherapps.FuenteApps
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Acciones sobre una aplicacion que combinan la fuente del sistema con la configuracion.
 *
 * Existe porque son operaciones que necesitan las dos fuentes a la vez (lanzar registra,
 * marcar favorito escribe en ajustes pero se dispara desde una lista del sistema) y las usan
 * tanto el Inicio como el Cajon. Es el unico punto por el que se lanza una app: cuando entre
 * el bloqueo por biometria, se intercepta aqui y funciona en todas las pantallas.
 */
@Singleton
class AccionesApp @Inject constructor(
    private val fuenteApps: FuenteApps,
    private val repositorio: RepositorioAjustes,
) {

    fun lanzar(app: AplicacionInstalada): Boolean = fuenteApps.lanzar(app)

    fun abrirInfo(app: AplicacionInstalada) = fuenteApps.abrirInfo(app)

    fun desinstalar(app: AplicacionInstalada) = fuenteApps.desinstalar(app)

    /** Agrega o quita de favoritos segun el estado actual. */
    suspend fun alternarFavorito(app: AplicacionInstalada, esFavorito: Boolean) {
        val favorito = app.aFavorito()
        if (esFavorito) repositorio.quitarFavorito(favorito) else repositorio.agregarFavorito(favorito)
    }

    /** Cambia el nombre visible. Un alias vacio restaura la etiqueta original. */
    suspend fun renombrar(app: AplicacionInstalada, alias: String?) {
        repositorio.renombrarFavorito(app.aFavorito(), alias)
    }

    /** Oculta o muestra la app en el cajon y la busqueda. */
    suspend fun alternarOculta(app: AplicacionInstalada) {
        repositorio.alternarOculta(app.claveEstable)
    }
}

/** Convierte una app del sistema en la referencia persistible que se guarda en ajustes. */
fun AplicacionInstalada.aFavorito(): FavoritoGuardado = FavoritoGuardado(
    paquete = paquete,
    clase = clase,
    serialUsuario = serialUsuario,
    // Se guarda el nombre para que el inicio pueda dibujarse sin esperar al sistema.
    etiquetaEnCache = etiqueta,
)

/**
 * Reconstruye una app lanzable a partir de un favorito guardado, sin consultar al sistema.
 *
 * Alcanza para dibujar la fila y para lanzarla (el lanzamiento solo necesita componente +
 * usuario). Los campos de busqueda quedan vacios porque un favorito no se busca.
 */
fun FavoritoGuardado.aAppDeCache(): AplicacionInstalada = AplicacionInstalada(
    paquete = paquete,
    clase = clase,
    serialUsuario = serialUsuario,
    etiqueta = etiquetaEnCache,
    etiquetaNormalizada = "",
    esPerfilTrabajo = false,
)

/** true si este favorito guardado apunta a esta app instalada. */
fun FavoritoGuardado.apuntaA(app: AplicacionInstalada): Boolean =
    paquete == app.paquete && clase == app.clase && serialUsuario == app.serialUsuario
