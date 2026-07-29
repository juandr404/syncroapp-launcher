package dev.syncroapp.launcher.core.launcherapps

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Estado de los gestos de navegacion del sistema, desde el punto de vista del launcher. */
enum class EstadoGestos {
    /** El equipo no necesita esta proteccion (no es Xiaomi). */
    NO_APLICA,

    /** MIUI los apago y no tenemos permiso para devolverlos. */
    APAGADOS_SIN_PERMISO,

    /** MIUI los apago pero podemos devolverlos. */
    APAGADOS_CON_PERMISO,

    /** Activos. */
    ACTIVOS,
}

/**
 * Devuelve los gestos de navegacion que MIUI apaga al usar un launcher externo.
 *
 * ## El problema
 *
 * MIUI/HyperOS desactiva los gestos de pantalla completa cuando el launcher predeterminado no
 * es el suyo, y deja el telefono en botones. No es un fallo de esta app: le pasa a todos los
 * launchers externos. Peor: no lo hace una sola vez. Se verifico en un Redmi Note 10 Pro que
 * MIUI revierte el ajuste `force_fsg_nav_bar` a 0 repetidamente, incluso despues de haberlo
 * puesto en 1 a mano por adb.
 *
 * ## La solucion
 *
 * Reponer el ajuste cada vez que el launcher vuelve al frente. Escribir en Settings.Global
 * exige WRITE_SECURE_SETTINGS, un permiso de sistema que NO se puede pedir con un dialogo:
 * se concede una sola vez por adb (ver [COMANDO_PARA_OTORGAR]). Ese requisito es justamente
 * lo que lo hace seguro.
 *
 * Sin el permiso, todo degrada con elegancia: la app funciona igual y Ajustes muestra el
 * comando exacto. Es opt-in ([activo]) para no pelear con quien de verdad prefiere botones.
 */
@Singleton
class GuardianDeGestos @Inject constructor(
    @ApplicationContext private val contexto: Context,
) {

    /** Solo las capas de Xiaomi tienen este comportamiento. */
    val aplicaEnEsteEquipo: Boolean =
        listOf(Build.MANUFACTURER, Build.BRAND).any { marca ->
            marca.equals("xiaomi", true) || marca.equals("redmi", true) || marca.equals("poco", true)
        }

    fun tienePermiso(): Boolean = contexto.checkSelfPermission(
        Manifest.permission.WRITE_SECURE_SETTINGS,
    ) == PackageManager.PERMISSION_GRANTED

    fun estado(): EstadoGestos = when {
        !aplicaEnEsteEquipo -> EstadoGestos.NO_APLICA
        gestosActivos() -> EstadoGestos.ACTIVOS
        tienePermiso() -> EstadoGestos.APAGADOS_CON_PERMISO
        else -> EstadoGestos.APAGADOS_SIN_PERMISO
    }

    private fun gestosActivos(): Boolean = runCatching {
        Settings.Global.getInt(contexto.contentResolver, AJUSTE_MIUI_GESTOS, 0) == 1
    }.getOrDefault(false)

    /**
     * Repone los gestos si hacen falta. Devuelve true si hubo algo que reponer y se logro.
     *
     * Se llama al volver al inicio, que es justo despues del momento en que MIUI suele
     * revertirlo (cambiar de launcher, abrir el selector de pantalla de inicio).
     */
    fun restaurarSiHaceFalta(): Boolean {
        if (!aplicaEnEsteEquipo || gestosActivos() || !tienePermiso()) return false

        return runCatching {
            Settings.Global.putInt(contexto.contentResolver, AJUSTE_MIUI_GESTOS, 1)
            Settings.Secure.putInt(contexto.contentResolver, AJUSTE_MODO_NAVEGACION, MODO_GESTOS)
            Log.i(TAG, "Gestos de navegacion restaurados tras un reinicio de MIUI")
            true
        }.getOrElse {
            Log.w(TAG, "No se pudieron restaurar los gestos", it)
            false
        }
    }

    companion object {
        /** Comando de un solo uso que habilita la proteccion. */
        const val COMANDO_PARA_OTORGAR =
            "adb shell pm grant dev.syncroapp.launcher android.permission.WRITE_SECURE_SETTINGS"

        private const val TAG = "GuardianDeGestos"

        /** Llave propia de MIUI: 1 = gestos de pantalla completa, 0 = botones. */
        private const val AJUSTE_MIUI_GESTOS = "force_fsg_nav_bar"

        /** Llave de AOSP: 0 = 3 botones, 1 = 2 botones, 2 = gestos. */
        private const val AJUSTE_MODO_NAVEGACION = "navigation_mode"
        private const val MODO_GESTOS = 2
    }
}
