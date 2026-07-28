package dev.syncroapp.launcher.core.data.ajustes

import androidx.datastore.core.DataStore
import dev.syncroapp.launcher.core.data.modelo.AjustesLauncher
import dev.syncroapp.launcher.core.data.modelo.FavoritoGuardado
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
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

    /** Configuracion actual. Ante un error de lectura de disco emite los defaults. */
    val ajustes: Flow<AjustesLauncher> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(AjustesLauncher()) else throw error
        }

    /** Actualiza la configuracion de forma atomica. */
    suspend fun actualizar(transformacion: (AjustesLauncher) -> AjustesLauncher) {
        dataStore.updateData(transformacion)
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

    /** Alterna el estado oculto de una app identificada por su clave estable. */
    suspend fun alternarOculta(claveEstable: String) = actualizar { actual ->
        val ocultas = actual.appsOcultas
        actual.copy(
            appsOcultas = if (claveEstable in ocultas) ocultas - claveEstable else ocultas + claveEstable,
        )
    }
}

/** Dos favoritos son el mismo si apuntan al mismo componente del mismo usuario (el alias no cuenta). */
private fun FavoritoGuardado.esMismoQue(otro: FavoritoGuardado): Boolean =
    paquete == otro.paquete && clase == otro.clase && serialUsuario == otro.serialUsuario
