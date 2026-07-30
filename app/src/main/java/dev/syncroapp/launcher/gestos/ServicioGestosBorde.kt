package dev.syncroapp.launcher.gestos

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import kotlin.math.abs

/**
 * PRUEBA MINIMA de gestos de navegacion propios.
 *
 * ## Por que existe
 *
 * En MIUI el unico proveedor de los gestos del sistema es el launcher de Xiaomi
 * (`com.miui.home/.recents.TouchInteractionService`). Con otro launcher predeterminado, atras y
 * recientes dejan de responder y no hay forma de arreglarlo desde una app normal. Ver
 * `GuardianDeGestos` para el diagnostico completo.
 *
 * Un servicio de accesibilidad SI puede ejecutar esas dos acciones, y ademas puede dibujar
 * ventanas de tipo [WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY], que no necesitan
 * el permiso de superposicion y viven por encima de cualquier app. Con eso se pueden poner
 * tiras invisibles en los bordes que detecten el deslizamiento desde dentro de otras apps.
 *
 * ## Alcance deliberado de esta version
 *
 * Solo atras y recientes; el inicio ya funciona porque este launcher ES la pantalla de inicio.
 * Sin ajustes, sin sensibilidad configurable y sin deteccion automatica de si los gestos del
 * sistema ya funcionan. La idea es comprobar en un telefono real que las tiras reciben el
 * toque y que las acciones se ejecutan, ANTES de invertir en el pulido.
 *
 * No modifica ningun ajuste del sistema: los botones de navegacion siguen donde esten. Se
 * prueba con los botones puestos, que es lo seguro.
 */
class ServicioGestosBorde : AccessibilityService() {

    private lateinit var ventanas: WindowManager
    private val tiras = mutableListOf<View>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        ventanas = getSystemService(WINDOW_SERVICE) as WindowManager

        val lateralIzquierdo = Gravity.START or Gravity.CENTER_VERTICAL
        val lateralDerecho = Gravity.END or Gravity.CENTER_VERTICAL
        val inferior = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL

        // Bordes izquierdo y derecho: deslizar hacia el centro = atras.
        agregarTira(lateralIzquierdo, ANCHO_LATERAL_PX, ALTO_LATERAL_PX) { g ->
            if (g.dx > UMBRAL_PX && abs(g.dx) > abs(g.dy)) Accion.ATRAS else null
        }
        agregarTira(lateralDerecho, ANCHO_LATERAL_PX, ALTO_LATERAL_PX) { g ->
            if (g.dx < -UMBRAL_PX && abs(g.dx) > abs(g.dy)) Accion.ATRAS else null
        }

        // Borde inferior: deslizar hacia arriba.
        //
        // Rapido va al inicio; sostenido muestra las apps abiertas, igual que en Android puro.
        // El discriminador es el tiempo que el dedo sigue abajo DESPUES de pasar el umbral: un
        // toque suelto lo cruza y suelta en unas decenas de milisegundos, mientras que sostener
        // (o arrastrar despacio a proposito) pasa de un cuarto de segundo.
        agregarTira(inferior, ANCHO_INFERIOR_PX, ALTO_INFERIOR_PX) { g ->
            when {
                g.dy >= -UMBRAL_PX || abs(g.dy) <= abs(g.dx) -> null
                g.msDesdeElUmbral >= MS_SOSTENER -> Accion.RECIENTES
                else -> Accion.INICIO
            }
        }

        Log.i(TAG, "Servicio de gestos conectado con ${tiras.size} tiras de borde")
    }

    /**
     * Agrega una tira invisible al borde indicado.
     *
     * [decidir] recibe el gesto completo y devuelve la accion a ejecutar, o null si no califica.
     */
    private fun agregarTira(
        gravedad: Int,
        ancho: Int,
        alto: Int,
        decidir: (Gesto) -> Accion?,
    ) {
        val parametros = WindowManager.LayoutParams(
            ancho,
            alto,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            // NOT_FOCUSABLE para no robarle el teclado a nadie; LAYOUT_NO_LIMITS para poder
            // ocupar la zona de los gestos del sistema y las esquinas de la pantalla.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply { this.gravity = gravedad }

        var inicioX = 0f
        var inicioY = 0f
        // 0 = el gesto todavia no ha pasado el umbral en esta caricia.
        var msDelUmbral = 0L

        val tira = View(this).apply {
            setOnTouchListener { vista, evento ->
                when (evento.action) {
                    MotionEvent.ACTION_DOWN -> {
                        inicioX = evento.rawX
                        inicioY = evento.rawY
                        msDelUmbral = 0L
                        true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        // Se anota el instante EXACTO en que el dedo cruzo el umbral, una sola
                        // vez por caricia. Medir desde ahi y no desde el inicio evita que un
                        // arranque titubeante cuente como haber sostenido el gesto.
                        val recorrido = maxOf(
                            abs(evento.rawX - inicioX),
                            abs(evento.rawY - inicioY),
                        )
                        if (msDelUmbral == 0L && recorrido > UMBRAL_PX) {
                            msDelUmbral = evento.eventTime
                        }
                        true
                    }

                    MotionEvent.ACTION_UP -> {
                        val gesto = Gesto(
                            dx = evento.rawX - inicioX,
                            dy = evento.rawY - inicioY,
                            msDesdeElUmbral = if (msDelUmbral == 0L) {
                                0L
                            } else {
                                evento.eventTime - msDelUmbral
                            },
                        )
                        val accion = decidir(gesto)
                        if (accion != null) {
                            // Vibracion corta: sin ella el usuario no sabe si el gesto entro.
                            // performHapticFeedback no necesita el permiso VIBRATE.
                            vista.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            ejecutar(accion)
                        }
                        true
                    }

                    else -> true
                }
            }
        }

        runCatching { ventanas.addView(tira, parametros) }
            .onSuccess { tiras += tira }
            .onFailure { Log.w(TAG, "No se pudo agregar la tira de borde", it) }
    }

    private fun ejecutar(accion: Accion) {
        val global = when (accion) {
            Accion.ATRAS -> GLOBAL_ACTION_BACK
            Accion.INICIO -> GLOBAL_ACTION_HOME
            Accion.RECIENTES -> GLOBAL_ACTION_RECENTS
        }
        val exito = performGlobalAction(global)
        Log.i(TAG, "Gesto $accion -> performGlobalAction exito=$exito")
    }

    override fun onDestroy() {
        tiras.forEach { tira -> runCatching { ventanas.removeView(tira) } }
        tiras.clear()
        super.onDestroy()
    }

    // Este servicio no lee el contenido de la pantalla: solo dibuja tiras y ejecuta acciones.
    // Por eso no hay nada que hacer con los eventos de accesibilidad.
    override fun onAccessibilityEvent(evento: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    /**
     * Un gesto terminado.
     *
     * @param dx desplazamiento horizontal total, negativo hacia la izquierda.
     * @param dy desplazamiento vertical total, negativo hacia arriba.
     * @param msDesdeElUmbral milisegundos que el dedo siguio apoyado despues de pasar el umbral.
     */
    private data class Gesto(val dx: Float, val dy: Float, val msDesdeElUmbral: Long)

    private enum class Accion { ATRAS, INICIO, RECIENTES }

    private companion object {
        const val TAG = "GestosBorde"

        // En pixeles crudos porque una LayoutParams de WindowManager no entiende dp.
        // A 440 dpi del Redmi, 1 dp = 2.75 px.
        const val ANCHO_LATERAL_PX = 55 // ~20 dp de ancho de zona sensible
        const val ALTO_LATERAL_PX = 1100 // franja central: deja libres las esquinas
        const val ANCHO_INFERIOR_PX = 700
        const val ALTO_INFERIOR_PX = 55
        const val UMBRAL_PX = 110 // ~40 dp: menos que esto es un toque, no un deslizamiento

        /** Sostener el dedo mas de un cuarto de segundo pasa de "ir al inicio" a "ver apps". */
        const val MS_SOSTENER = 250L
    }
}
