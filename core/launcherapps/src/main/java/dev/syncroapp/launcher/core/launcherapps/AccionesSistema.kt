package dev.syncroapp.launcher.core.launcherapps

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.util.Log
import android.widget.Toast
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

    /**
     * Abre la app de reloj/alarmas (toque sobre la hora).
     *
     * La accion estandar de alarmas no siempre esta declarada: MIUI, por ejemplo, no la atiende.
     * Por eso, si falla, se abre directamente la app de reloj del fabricante.
     */
    fun abrirReloj(): Boolean = abrirLoPrimeroQueFuncione(
        "el reloj",
        intents = listOf(Intent(AlarmClock.ACTION_SHOW_ALARMS)),
        paquetesDeRespaldo = PAQUETES_RELOJ,
    )

    /** Abre el calendario en el dia de hoy (toque sobre la fecha). */
    fun abrirCalendario(): Boolean = abrirLoPrimeroQueFuncione(
        "el calendario",
        intents = listOf(
            Intent(Intent.ACTION_VIEW)
                .setData(CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build()),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR),
        ),
        paquetesDeRespaldo = PAQUETES_CALENDARIO,
    )

    /**
     * Prueba las acciones estandar y, si ninguna funciona, abre por nombre de paquete las apps
     * conocidas de cada fabricante.
     *
     * Si aun asi no hay nada, se lo dice al usuario: un toque que no produce ninguna reaccion
     * se siente como una app rota.
     */
    private fun abrirLoPrimeroQueFuncione(
        queCosa: String,
        intents: List<Intent>,
        paquetesDeRespaldo: List<String>,
    ): Boolean {
        for (intent in intents) {
            if (intentar(intent)) return true
        }

        for (paquete in paquetesDeRespaldo) {
            val intent = contexto.packageManager.getLaunchIntentForPackage(paquete) ?: continue
            if (intentar(intent)) return true
        }

        Toast.makeText(contexto, "No se encontro una app para $queCosa.", Toast.LENGTH_SHORT).show()
        return false
    }

    private fun intentar(intent: Intent): Boolean = runCatching {
        contexto.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    }.getOrElse {
        Log.d(TAG, "Nadie atiende ${intent.action}", it)
        false
    }

    private companion object {
        const val TAG = "AccionesSistema"

        val PAQUETES_RELOJ = listOf(
            "com.android.deskclock", // MIUI y AOSP
            "com.google.android.deskclock", // Reloj de Google
            "com.sec.android.app.clockpackage", // Samsung
        )

        val PAQUETES_CALENDARIO = listOf(
            "com.android.calendar",
            "com.google.android.calendar",
            "com.xiaomi.calendar",
        )
    }
}
