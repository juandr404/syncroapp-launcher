package dev.syncroapp.launcher.core.launcherapps

import kotlinx.coroutines.flow.StateFlow

/**
 * Frontera con las APIs de sistema para listar y lanzar aplicaciones.
 *
 * Toda la app habla con esta interfaz y nunca con LauncherApps directamente: eso permite
 * sustituirla por un fake en los tests y concentrar en un solo sitio los workarounds de OEMs.
 */
interface FuenteApps {

    /**
     * Lista de apps lanzables, ya ordenada alfabeticamente.
     *
     * Es la unica fuente de verdad: se llena al arrancar el proceso y se invalida sola
     * cuando el sistema avisa que se instalo, desinstalo o cambio un paquete.
     */
    val apps: StateFlow<List<AplicacionInstalada>>

    /** true si la lista inicial ya termino de cargar (para no parpadear "vacio" al arrancar). */
    val cargaInicialCompletada: StateFlow<Boolean>

    /**
     * Lanza la app. Devuelve false si ya no existe (se desinstalo entre el dibujo y el toque),
     * en cuyo caso la lista se refresca sola.
     */
    fun lanzar(app: AplicacionInstalada): Boolean

    /** Abre la pantalla de informacion de la app en Ajustes del sistema. */
    fun abrirInfo(app: AplicacionInstalada)

    /** Lanza el flujo de desinstalacion del sistema. */
    fun desinstalar(app: AplicacionInstalada)

    /** Fuerza una recarga completa de la lista. */
    fun refrescar()
}
