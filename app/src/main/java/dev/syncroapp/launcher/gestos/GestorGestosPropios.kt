package dev.syncroapp.launcher.gestos

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Estado del servicio de gestos propios y acceso a la pantalla del sistema para activarlo.
 *
 * El servicio de accesibilidad NO se puede activar desde codigo: eso es intencional en Android
 * y es lo correcto, porque es un permiso potente. Lo unico que puede hacer la app es llevar al
 * usuario a la pantalla donde lo activa a mano.
 */
@Singleton
class GestorGestosPropios @Inject constructor(
    @ApplicationContext private val contexto: Context,
) {

    private val componente = ComponentName(contexto, ServicioGestosBorde::class.java)

    /**
     * true si el usuario ya habilito el servicio.
     *
     * Se lee de la lista de servicios habilitados del sistema y no de un ajuste propio: la
     * fuente de verdad es el sistema, y el usuario puede apagarlo desde ahi sin avisarnos.
     */
    fun estaActivo(): Boolean {
        val habilitados = Settings.Secure.getString(
            contexto.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()

        val nombrePlano = componente.flattenToString()
        val nombreCorto = componente.flattenToShortString()

        return habilitados.split(':').any { it == nombrePlano || it == nombreCorto }
    }

    /** Abre los ajustes de accesibilidad del sistema para que el usuario lo active o desactive. */
    fun intentAjustesAccesibilidad(): Intent =
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /**
     * Pantalla de inicio automatico de MIUI, si este equipo la tiene.
     *
     * Sin ese permiso, Xiaomi apaga el servicio de accesibilidad en cada reinicio: el usuario
     * enciende el telefono y se encuentra sin gestos, con los botones de vuelta. Verificado en
     * un Redmi Note 10 Pro, y es la causa numero uno de que los gestos "dejen de funcionar solos".
     *
     * Devuelve null en equipos que no son Xiaomi o si la actividad no existe en esta version.
     */
    fun intentInicioAutomatico(): Intent? {
        val intent = Intent().setClassName(PAQUETE_SEGURIDAD_MIUI, ACTIVIDAD_INICIO_AUTOMATICO)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        // Se comprueba que exista antes de ofrecerla: un boton que lleva a una pantalla que no
        // abre es peor que no tener el boton.
        val existe = contexto.packageManager
            .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            .isNotEmpty()

        return intent.takeIf { existe }
    }

    /** Ficha de la app en los ajustes del sistema, donde vive la restriccion de bateria. */
    fun intentDetallesDeLaApp(): Intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", contexto.packageName, null),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private companion object {
        const val PAQUETE_SEGURIDAD_MIUI = "com.miui.securitycenter"
        const val ACTIVIDAD_INICIO_AUTOMATICO =
            "com.miui.permcenter.autostart.AutoStartManagementActivity"
    }
}
