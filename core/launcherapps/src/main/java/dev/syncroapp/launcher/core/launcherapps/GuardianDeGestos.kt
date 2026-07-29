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

/** Estado de la navegacion del sistema, desde el punto de vista del launcher. */
enum class EstadoGestos {
    /** El equipo no tiene esta limitacion (no es Xiaomi). */
    NO_APLICA,

    /** En botones: es el estado sano en MIUI con un launcher externo. */
    EN_BOTONES,

    /**
     * Gestos encendidos pero sin nadie que los atienda: el telefono se queda sin botones Y sin
     * gestos. Es el estado peligroso, y hay que ofrecer la salida.
     */
    ACTIVOS_PERO_SIN_MANEJADOR,
}

/**
 * Lee el estado de los gestos de navegacion. **Ya no los fuerza.**
 *
 * ## Por que se dejo de forzarlos
 *
 * La primera version encendia `force_fsg_nav_bar` para devolver los gestos que MIUI apaga al
 * poner un launcher externo. El resultado fue peor que el problema: los botones desaparecian
 * pero los gestos seguian sin responder, y el usuario quedaba encerrado dentro de una app,
 * sin forma de volver atras ni de ver las apps abiertas.
 *
 * ## La causa real (diagnosticada en un Redmi Note 10 Pro, MIUI 14 / Android 13)
 *
 * En este telefono el UNICO proveedor de `android.intent.action.QUICKSTEP_SERVICE` es
 * `com.miui.home/.recents.TouchInteractionService`, es decir el launcher de Xiaomi. Ese
 * servicio es el que implementa los gestos de atras, recientes e inicio. SystemUI se mantiene
 * enlazado a el incluso con otro launcher predeterminado, pero deja de atender los gestos, y
 * MIUI no incluye un proveedor de reemplazo como si hace Android puro.
 *
 * Conclusion: **ninguna app puede hacer que los gestos del sistema funcionen con un launcher
 * externo en MIUI.** Encender el ajuste solo oculta los botones y deja el telefono sin ninguna
 * forma de navegar. Por eso este archivo ahora solo informa.
 *
 * En Android puro (Pixel) no aplica: Quickstep sigue sirviendo los gestos aunque el launcher
 * predeterminado sea de terceros.
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

    fun estado(): EstadoGestos = when {
        !aplicaEnEsteEquipo -> EstadoGestos.NO_APLICA
        gestosActivos() -> EstadoGestos.ACTIVOS_PERO_SIN_MANEJADOR
        else -> EstadoGestos.EN_BOTONES
    }

    private fun gestosActivos(): Boolean = runCatching {
        Settings.Global.getInt(contexto.contentResolver, AJUSTE_MIUI_GESTOS, 0) == 1
    }.getOrDefault(false)

    /**
     * Devuelve el telefono a botones.
     *
     * Es la unica escritura que queda, y existe como salida de emergencia: si el usuario quedo
     * con los gestos encendidos y sin manejador (por la version anterior de esta app o por
     * cualquier otra via), esto le devuelve una forma de navegar. Requiere el permiso; sin el,
     * Ajustes muestra la ruta manual.
     */
    fun volverABotones(): Boolean {
        if (!tienePermiso()) return false

        return runCatching {
            Settings.Global.putInt(contexto.contentResolver, AJUSTE_MIUI_GESTOS, 0)
            Settings.Secure.putInt(contexto.contentResolver, AJUSTE_MODO_NAVEGACION, MODO_BOTONES)
            Log.i(TAG, "Navegacion devuelta a botones")
            true
        }.getOrElse {
            Log.w(TAG, "No se pudo volver a botones", it)
            false
        }
    }

    fun tienePermiso(): Boolean = contexto.checkSelfPermission(
        Manifest.permission.WRITE_SECURE_SETTINGS,
    ) == PackageManager.PERMISSION_GRANTED

    companion object {
        /** Comando de un solo uso que habilita la salida de emergencia a botones. */
        const val COMANDO_PARA_OTORGAR =
            "adb shell pm grant dev.syncroapp.launcher android.permission.WRITE_SECURE_SETTINGS"

        private const val TAG = "GuardianDeGestos"

        /** Llave propia de MIUI: 1 = gestos de pantalla completa, 0 = botones. */
        private const val AJUSTE_MIUI_GESTOS = "force_fsg_nav_bar"

        /** Llave de AOSP: 0 = 3 botones, 1 = 2 botones, 2 = gestos. */
        private const val AJUSTE_MODO_NAVEGACION = "navigation_mode"
        private const val MODO_BOTONES = 0
    }
}
