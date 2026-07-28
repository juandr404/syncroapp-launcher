package dev.syncroapp.launcher.core.launcherapps

/**
 * Una aplicacion lanzable instalada en el dispositivo.
 *
 * Se identifica por componente + usuario, nunca solo por paquete: en un perfil de trabajo
 * el mismo paquete existe dos veces con [serialUsuario] distinto y son apps diferentes.
 *
 * @param serialUsuario numero de serie del UserHandle (UserManager.getSerialNumberForUser).
 *        Se guarda el serial y no el UserHandle porque el serial sobrevive a reinicios.
 * @param etiquetaNormalizada etiqueta en minusculas y sin tildes, precalculada una sola vez
 *        para que la busqueda no normalice 300 nombres en cada tecla.
 */
data class AplicacionInstalada(
    val paquete: String,
    val clase: String,
    val serialUsuario: Long,
    val etiqueta: String,
    val etiquetaNormalizada: String,
    val esPerfilTrabajo: Boolean,
) {
    /** Identificador estable para persistir en ajustes (favoritos, apps ocultas). */
    val claveEstable: String = "$paquete/$clase#$serialUsuario"
}
