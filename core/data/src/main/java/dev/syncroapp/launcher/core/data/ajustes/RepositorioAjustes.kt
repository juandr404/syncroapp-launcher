package dev.syncroapp.launcher.core.data.ajustes

import androidx.datastore.core.DataStore
import dev.syncroapp.launcher.core.data.modelo.AjustesLauncher
import dev.syncroapp.launcher.core.data.modelo.EstiloIconos
import dev.syncroapp.launcher.core.data.modelo.FavoritoGuardado
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unico punto de lectura y escritura de la configuracion del launcher.
 *
 * Expone un [Flow] del objeto completo: los ViewModels lo combinan con otras fuentes
 * y derivan su estado, nunca mutan configuracion parcial a mano.
 */
@Singleton
class RepositorioAjustes @Inject constructor(
    private val dataStore: DataStore<AjustesLauncher>,
) {

    /** Configuracion actual, ya migrada. Ante un error de lectura de disco emite los defaults. */
    val ajustes: Flow<AjustesLauncher> = dataStore.data
        .map(::migrar)
        .catch { error ->
            if (error is IOException) emit(AjustesLauncher()) else throw error
        }

    /** Actualiza la configuracion de forma atomica, migrando antes de transformar. */
    suspend fun actualizar(transformacion: (AjustesLauncher) -> AjustesLauncher) {
        dataStore.updateData { transformacion(migrar(it)) }
    }

    /**
     * Migraciones de esquema. Idempotente: un archivo ya migrado pasa sin cambios.
     *
     * v0 -> v1: "sin iconos" en el cajon era el valor por defecto, no una eleccion del usuario
     * (la opcion ni siquiera existia en Ajustes). El nuevo defecto son los iconos originales,
     * asi que los archivos viejos se actualizan para que el cambio les llegue.
     */
    private fun migrar(guardado: AjustesLauncher): AjustesLauncher {
        if (guardado.versionAjustes >= VERSION_ACTUAL) return guardado

        var migrado = guardado

        // v0 -> v1
        if (migrado.versionAjustes < 1 && migrado.estiloIconos == EstiloIconos.NINGUNO) {
            migrado = migrado.copy(estiloIconos = EstiloIconos.ORIGINALES)
        }

        // v1 -> v2: el dock nace vacio. No se rellena solo con apps adivinadas; que aparezcan
        // cinco circulos sin que el usuario los pidiera seria imponerle una decision.

        return migrado.copy(versionAjustes = VERSION_ACTUAL)
    }

    // --- Favoritos ---

    /** Agrega un favorito al final de la lista si hay cupo y no esta repetido. */
    suspend fun agregarFavorito(favorito: FavoritoGuardado) = actualizar { actual ->
        val yaExiste = actual.favoritos.any { it.esMismoQue(favorito) }
        if (yaExiste || actual.favoritos.size >= AjustesLauncher.MAX_FAVORITOS) {
            actual
        } else {
            actual.copy(favoritos = actual.favoritos + favorito)
        }
    }

    /** Quita un favorito por componente + usuario. */
    suspend fun quitarFavorito(favorito: FavoritoGuardado) = actualizar { actual ->
        actual.copy(favoritos = actual.favoritos.filterNot { it.esMismoQue(favorito) })
    }

    // --- Dock inferior ---

    /** Alterna una app en el dock: la agrega si hay cupo, la quita si ya estaba. */
    suspend fun alternarEnDock(acceso: FavoritoGuardado) = actualizar { actual ->
        val yaEsta = actual.dock.any { it.esMismoQue(acceso) }
        when {
            yaEsta -> actual.copy(dock = actual.dock.filterNot { it.esMismoQue(acceso) })
            actual.dock.size >= AjustesLauncher.MAX_DOCK -> actual
            else -> actual.copy(dock = actual.dock + acceso)
        }
    }

    /** Reordena la lista completa de favoritos (la UI envia el orden resultante del arrastre). */
    suspend fun reordenarFavoritos(nuevoOrden: List<FavoritoGuardado>) = actualizar { actual ->
        actual.copy(favoritos = nuevoOrden.take(AjustesLauncher.MAX_FAVORITOS))
    }

    /** Cambia el nombre visible de un favorito (null restaura la etiqueta original). */
    suspend fun renombrarFavorito(favorito: FavoritoGuardado, alias: String?) = actualizar { actual ->
        actual.copy(
            favoritos = actual.favoritos.map {
                if (it.esMismoQue(favorito)) it.copy(alias = alias?.takeIf(String::isNotBlank)) else it
            },
        )
    }

    // --- Apps ocultas ---

    /**
     * Pone al dia los nombres guardados de los favoritos.
     *
     * Cumple dos funciones: rellena los favoritos que se guardaron antes de que existiera la
     * cache de nombres, y refresca los que cambiaron de nombre al actualizarse la app.
     *
     * @param etiquetasFrescas nombre real de cada app, por clave "paquete/clase/serial".
     */
    suspend fun sincronizarEtiquetas(etiquetasFrescas: Map<String, String>) {
        val actuales = dataStore.data.first().favoritos
        val hayCambios = actuales.any { favorito ->
            val fresca = etiquetasFrescas[favorito.claveDeFavorito()]
            fresca != null && fresca != favorito.etiquetaEnCache
        }
        // Sin esta guarda, cada emision del flujo dispararia una escritura y otra emision.
        if (!hayCambios) return

        actualizar { actual ->
            actual.copy(
                favoritos = actual.favoritos.map { favorito ->
                    val fresca = etiquetasFrescas[favorito.claveDeFavorito()]
                    if (fresca != null) favorito.copy(etiquetaEnCache = fresca) else favorito
                },
            )
        }
    }

    /** Alterna el estado oculto de una app identificada por su clave estable. */
    suspend fun alternarOculta(claveEstable: String) = actualizar { actual ->
        val ocultas = actual.appsOcultas
        actual.copy(
            appsOcultas = if (claveEstable in ocultas) ocultas - claveEstable else ocultas + claveEstable,
        )
    }

    private companion object {
        /** Subir en uno cada vez que se agregue un paso a [migrar]. */
        const val VERSION_ACTUAL = 2
    }
}

/** Dos favoritos son el mismo si apuntan al mismo componente del mismo usuario (el alias no cuenta). */
private fun FavoritoGuardado.esMismoQue(otro: FavoritoGuardado): Boolean =
    paquete == otro.paquete && clase == otro.clase && serialUsuario == otro.serialUsuario

/**
 * Clave con el MISMO formato que `AplicacionInstalada.claveEstable`, para poder cruzarlas.
 * Si el formato de una cambia, tiene que cambiar el de la otra.
 */
fun FavoritoGuardado.claveDeFavorito(): String = "$paquete/$clase#$serialUsuario"
