package dev.syncroapp.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dagger.hilt.android.AndroidEntryPoint
import dev.syncroapp.launcher.navegacion.Pantalla
import dev.syncroapp.launcher.ui.RaizLauncher

/**
 * La actividad HOME.
 *
 * Contratos que un launcher no puede romper:
 *  - Nunca llama a finish(): si se cierra, el usuario se queda sin pantalla de inicio.
 *  - El boton/gesto Home llega como onNewIntent (launchMode singleTask) y debe devolver
 *    siempre al estado raiz: es el escape universal del usuario.
 *  - No guarda estado en savedInstanceState (stateNotNeeded): el estado real vive en
 *    DataStore, asi que una recreacion por parte del sistema es invisible.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var pantallaActual by mutableStateOf(Pantalla.INICIO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            RaizLauncher(
                pantallaActual = pantallaActual,
                onCambiarPantalla = { pantallaActual = it },
            )
        }
    }

    /**
     * Llega cada vez que el usuario presiona Home estando ya en el launcher.
     * Resetea al estado raiz: cierra el cajon, limpia la busqueda, vuelve a Inicio.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pantallaActual = Pantalla.INICIO
    }
}
