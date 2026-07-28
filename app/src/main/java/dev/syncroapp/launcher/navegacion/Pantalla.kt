package dev.syncroapp.launcher.navegacion

/**
 * Pantallas del launcher. Son tres y no cambian: no hace falta un grafo de navegacion
 * con rutas de texto para modelar esto.
 */
enum class Pantalla {
    /** Reloj + favoritos. Estado raiz al que siempre vuelve el boton Home. */
    INICIO,

    /** Cajon de aplicaciones con busqueda. */
    CAJON,

    /** Configuracion. */
    AJUSTES,
}
