package dev.syncroapp.launcher.core.launcherapps

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Acciones sueltas contra el sistema que no encajan en [FuenteApps].
 * Todas degradan en silencio: ninguna es critica para que el launcher siga siendo usable.
 */
@Singleton
class AccionesSistema @Inject constructor(
    @ApplicationContext private val contexto: Context,
) {

    /**
     * Despliega el panel de notificaciones (gesto swipe abajo).
     *
     * No existe API publica para esto. Se intenta por reflexion sobre StatusBarManager, que
     * funciona en muchos dispositivos pero esta bloqueado por las restricciones de interfaces
     * no-SDK en varias versiones y capas. Si falla, no pasa nada: el usuario siempre puede
     * usar el gesto nativo del sistema desde el borde superior.
     *
     * @return true si el panel se abrio.
     */
    fun expandirPanelNotificaciones(): Boolean = runCatching {
        val servicio = contexto.getSystemService("statusbar")
        val metodo = Class.forName("android.app.StatusBarManager")
            .getMethod("expandNotificationsPanel")
        metodo.invoke(servicio)
        true
    }.getOrElse {
        Log.d(TAG, "Panel de notificaciones no disponible en este dispositivo", it)
        false
    }

    /** Abre la app de reloj/alarmas del sistema (toque sobre la hora). */
    fun abrirReloj(): Boolean = abrir(
        Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )

    /** Abre el calendario en el dia de hoy (toque sobre la fecha). */
    fun abrirCalendario(): Boolean = abrir(
        Intent(Intent.ACTION_VIEW)
            .setData(CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )

    private fun abrir(intent: Intent): Boolean = runCatching {
        contexto.startActivity(intent)
        true
    }.getOrElse {
        Log.d(TAG, "No hay app que atienda ${intent.action}", it)
        false
    }

    private companion object {
        const val TAG = "AccionesSistema"
    }
}
