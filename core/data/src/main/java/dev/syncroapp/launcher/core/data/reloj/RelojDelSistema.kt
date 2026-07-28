package dev.syncroapp.launcher.core.data.reloj

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fuente de la hora actual para el reloj del Home.
 *
 * Emite cada minuto usando el broadcast ACTION_TIME_TICK del sistema, no un temporizador propio:
 * cero wakelocks, cero polling, y el tick llega exactamente cuando cambia el minuto (sin drift).
 *
 * El receiver solo existe mientras alguien colecta el flow, y la UI colecta solo en STARTED,
 * asi que en background el launcher no consume nada.
 */
@Singleton
class RelojDelSistema @Inject constructor(
    @ApplicationContext private val contexto: Context,
) {

    val instante: Flow<LocalDateTime> = callbackFlow {
        // Emision inmediata: la UI no debe esperar hasta el proximo cambio de minuto.
        trySend(LocalDateTime.now())

        val receptor = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                trySend(LocalDateTime.now())
            }
        }

        val filtro = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)      // cada cambio de minuto
            addAction(Intent.ACTION_TIME_CHANGED)   // el usuario cambio la hora
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_LOCALE_CHANGED) // cambio de idioma: cambia el nombre del dia
        }

        ContextCompat.registerReceiver(
            contexto,
            receptor,
            filtro,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        awaitClose { contexto.unregisterReceiver(receptor) }
    }.conflate()
}
