package dev.syncroapp.launcher.core.data.ajustes

import dev.syncroapp.launcher.core.data.modelo.FavoritoGuardado
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Protege el cruce entre un favorito guardado y una app instalada.
 *
 * Este test existe por un bug real: `claveDeFavorito()` armaba la clave con "/" antes del
 * serial mientras que `AplicacionInstalada.claveEstable` usa "#". Las dos claves nunca
 * coincidian, asi que la sincronizacion de nombres no hacia nada -- sin lanzar ningun error,
 * sin fallar ninguna compilacion. Un formato duplicado en dos modulos distintos solo se
 * detecta con una prueba que fije el formato esperado.
 *
 * Si `AplicacionInstalada.claveEstable` cambia de forma, este test debe fallar.
 */
class ClaveDeFavoritoTest {

    @Test
    fun `la clave usa numeral antes del serial de usuario`() {
        val favorito = FavoritoGuardado(
            paquete = "com.whatsapp",
            clase = "com.whatsapp.Main",
            serialUsuario = 0,
        )

        assertEquals("com.whatsapp/com.whatsapp.Main#0", favorito.claveDeFavorito())
    }

    @Test
    fun `dos perfiles del mismo componente producen claves distintas`() {
        val personal = FavoritoGuardado("com.app", "com.app.Main", serialUsuario = 0)
        val trabajo = personal.copy(serialUsuario = 10)

        assertEquals("com.app/com.app.Main#0", personal.claveDeFavorito())
        assertEquals("com.app/com.app.Main#10", trabajo.claveDeFavorito())
    }

    @Test
    fun `el alias y el nombre en cache no afectan la clave`() {
        val base = FavoritoGuardado("com.app", "com.app.Main", serialUsuario = 0)
        val personalizado = base.copy(alias = "mi app", etiquetaEnCache = "Aplicacion")

        assertEquals(base.claveDeFavorito(), personalizado.claveDeFavorito())
    }
}
