package dev.syncroapp.launcher.core.data.modelo

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Compatibilidad de la configuracion guardada en disco.
 *
 * Un launcher se actualiza encima de si mismo: el archivo que escribio la version anterior
 * TIENE que poder leerse con la nueva. Estas pruebas fijan ese contrato, porque la alternativa
 * es que un usuario pierda sus favoritos en una actualizacion.
 */
class AjustesSerializacionTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `un archivo de la primera version se lee sin perder favoritos`() {
        // Archivo real escrito por v0.1: no tiene estiloReloj, tamanoDia ni etiquetaEnCache.
        val archivoViejo = """
            {
              "tema": "DARK_PURO",
              "formato24h": true,
              "favoritos": [
                { "paquete": "com.whatsapp", "clase": "com.whatsapp.Main", "serialUsuario": 0 }
              ]
            }
        """.trimIndent()

        val ajustes = json.decodeFromString<AjustesLauncher>(archivoViejo)

        assertEquals(1, ajustes.favoritos.size)
        assertEquals("com.whatsapp", ajustes.favoritos.first().paquete)
    }

    @Test
    fun `los campos nuevos toman su valor por defecto en archivos viejos`() {
        val ajustes = json.decodeFromString<AjustesLauncher>("""{ "formato24h": false }""")

        assertEquals(EstiloReloj.RELOJ_GRANDE, ajustes.estiloReloj)
        assertEquals(TamanoDia.MEDIANO, ajustes.tamanoDia)
        assertEquals("", ajustes.favoritos.firstOrNull()?.etiquetaEnCache ?: "")
        // versionAjustes 0 es lo que dispara la migracion en RepositorioAjustes.
        assertEquals(0, ajustes.versionAjustes)
    }

    @Test
    fun `una llave desconocida de una version futura no rompe la lectura`() {
        val delFuturo = """{ "tema": "CLARO", "ajusteQueTodaviaNoExiste": 42 }"""

        val ajustes = json.decodeFromString<AjustesLauncher>(delFuturo)

        assertEquals(Tema.CLARO, ajustes.tema)
    }

    @Test
    fun `la configuracion completa sobrevive un ciclo de escritura y lectura`() {
        val original = AjustesLauncher(
            tema = Tema.DARK_SUAVE,
            estiloReloj = EstiloReloj.DIA_GIGANTE,
            tamanoDia = TamanoDia.GRANDE,
            estiloIconos = EstiloIconos.MONOCROMO,
            versionAjustes = 1,
            favoritos = listOf(
                FavoritoGuardado("com.app", "com.app.Main", 0, alias = "mi app", etiquetaEnCache = "App"),
            ),
            appsOcultas = setOf("com.oculta/com.oculta.Main#0"),
        )

        val texto = json.encodeToString(AjustesLauncher.serializer(), original)
        val recuperado = json.decodeFromString<AjustesLauncher>(texto)

        assertEquals(original, recuperado)
    }

    @Test
    fun `el limite de favoritos es coherente con el maximo declarado`() {
        assertTrue(
            "MAX_FAVORITOS debe ser positivo para que el inicio pueda mostrar algo",
            AjustesLauncher.MAX_FAVORITOS > 0,
        )
    }
}
