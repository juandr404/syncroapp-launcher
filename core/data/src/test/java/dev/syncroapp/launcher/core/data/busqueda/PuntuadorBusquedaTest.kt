package dev.syncroapp.launcher.core.data.busqueda

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PuntuadorBusquedaTest {

    // --- Normalizacion ---

    @Test
    fun `normalizar quita tildes y pasa a minusculas`() {
        assertEquals("camara", PuntuadorBusqueda.normalizar("Cámara"))
        assertEquals("telefono", PuntuadorBusqueda.normalizar("TELÉFONO"))
        assertEquals("musica", PuntuadorBusqueda.normalizar("Música"))
    }

    @Test
    fun `buscar sin tildes encuentra apps con tildes`() {
        val etiqueta = PuntuadorBusqueda.normalizar("Cámara")
        assertTrue(PuntuadorBusqueda.puntuar("camara", etiqueta) > 0)
        assertTrue(PuntuadorBusqueda.puntuar("cam", etiqueta) > 0)
    }

    // --- Cascada de puntajes ---

    @Test
    fun `coincidencia exacta gana sobre prefijo`() {
        val exacta = PuntuadorBusqueda.puntuar("spotify", "spotify")
        val prefijo = PuntuadorBusqueda.puntuar("spot", "spotify")
        assertTrue("exacta=$exacta prefijo=$prefijo", exacta > prefijo)
    }

    @Test
    fun `prefijo de etiqueta gana sobre prefijo de palabra interna`() {
        val inicio = PuntuadorBusqueda.puntuar("goo", "google maps")
        val interna = PuntuadorBusqueda.puntuar("map", "google maps")
        assertTrue("inicio=$inicio interna=$interna", inicio > interna)
    }

    @Test
    fun `iniciales encuentran apps de varias palabras`() {
        assertTrue(PuntuadorBusqueda.puntuar("gm", "google maps") > 0)
        assertTrue(PuntuadorBusqueda.puntuar("wb", "whatsapp business") > 0)
    }

    @Test
    fun `subsecuencia dispersa coincide pero puntua bajo`() {
        val dispersa = PuntuadorBusqueda.puntuar("sptfy", "spotify")
        val prefijo = PuntuadorBusqueda.puntuar("spo", "spotify")
        assertTrue("dispersa=$dispersa", dispersa > 0)
        assertTrue("dispersa=$dispersa prefijo=$prefijo", prefijo > dispersa)
    }

    @Test
    fun `consulta que no aparece no coincide`() {
        assertEquals(PuntuadorBusqueda.SIN_COINCIDENCIA, PuntuadorBusqueda.puntuar("zzz", "spotify"))
    }

    @Test
    fun `consulta vacia no filtra nada`() {
        assertEquals(0, PuntuadorBusqueda.puntuar("", "spotify"))
    }

    // --- Desempates ---

    @Test
    fun `ante igual tipo de coincidencia gana el nombre mas corto`() {
        val corto = PuntuadorBusqueda.puntuar("what", "whatsapp")
        val largo = PuntuadorBusqueda.puntuar("what", "whatsapp business messenger")
        assertTrue("corto=$corto largo=$largo", corto > largo)
    }

    @Test
    fun `ordenar por puntaje pone lo mas relevante primero`() {
        val apps = listOf("google maps", "maps me", "mapbox studio", "spotify")
        val ordenadas = apps
            .map { it to PuntuadorBusqueda.puntuar("map", it) }
            .filter { it.second != PuntuadorBusqueda.SIN_COINCIDENCIA }
            .sortedByDescending { it.second }
            .map { it.first }

        assertEquals(listOf("maps me", "mapbox studio", "google maps"), ordenadas)
    }
}
