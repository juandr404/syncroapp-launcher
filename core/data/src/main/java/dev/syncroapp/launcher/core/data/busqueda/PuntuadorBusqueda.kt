package dev.syncroapp.launcher.core.data.busqueda

import java.text.Normalizer
import java.util.Locale

/**
 * Puntuador de coincidencias para la busqueda de aplicaciones.
 *
 * Es logica pura (sin dependencias de Android) para que sea barata de testear: la busqueda es
 * el flujo mas usado del launcher y el que mas se nota cuando ordena mal.
 *
 * La cascada de puntajes va de la coincidencia mas literal a la mas laxa:
 *   1000  etiqueta exacta                    "spotify"  -> Spotify
 *    900  la etiqueta empieza con la consulta "spot"     -> Spotify
 *    800  una palabra empieza con la consulta "maps"     -> Google Maps
 *    700  iniciales de las palabras           "gm"       -> Google Maps
 *    600  contiene la consulta                "tify"     -> Spotify
 *    400  subsecuencia dispersa               "sptfy"    -> Spotify
 *     -1  no coincide
 *
 * Los desempates restan puntos por posicion de la coincidencia y por longitud de la etiqueta,
 * de modo que ante igualdad gana el nombre mas corto y la coincidencia mas al principio.
 */
object PuntuadorBusqueda {

    const val SIN_COINCIDENCIA = -1

    private val REGEX_DIACRITICOS = Regex("\\p{Mn}+")
    private val REGEX_SEPARADORES = Regex("[\\s\\-_.]+")

    /**
     * Normaliza texto para comparar: minusculas y sin tildes.
     * Critico para espanol: "camara" debe encontrar "Cámara".
     */
    fun normalizar(texto: String): String =
        REGEX_DIACRITICOS
            .replace(Normalizer.normalize(texto, Normalizer.Form.NFD), "")
            .lowercase(Locale.ROOT)
            .trim()

    /**
     * Puntua una etiqueta contra una consulta. Ambas deben venir ya normalizadas
     * con [normalizar] — normalizar en cada tecleo seria trabajo repetido.
     *
     * @return puntaje mayor = mejor coincidencia, o [SIN_COINCIDENCIA] si no coincide.
     */
    fun puntuar(consulta: String, etiqueta: String): Int {
        if (consulta.isEmpty()) return 0
        if (etiqueta.isEmpty()) return SIN_COINCIDENCIA

        // Penalizacion suave por longitud: ante igualdad gana el nombre mas corto.
        val penalizacionLongitud = (etiqueta.length / 8).coerceAtMost(20)

        if (etiqueta == consulta) return 1000

        if (etiqueta.startsWith(consulta)) return 900 - penalizacionLongitud

        val palabras = etiqueta.split(REGEX_SEPARADORES).filter(String::isNotEmpty)

        val indicePalabra = palabras.indexOfFirst { it.startsWith(consulta) }
        if (indicePalabra >= 0) return 800 - indicePalabra * 5 - penalizacionLongitud

        // Iniciales: "gm" encuentra "Google Maps", "wa" encuentra "WhatsApp Business".
        if (consulta.length >= 2 && palabras.size >= 2) {
            val iniciales = palabras.joinToString("") { it.first().toString() }
            if (iniciales.startsWith(consulta)) return 700 - penalizacionLongitud
        }

        val indiceContiene = etiqueta.indexOf(consulta)
        if (indiceContiene > 0) return 600 - indiceContiene.coerceAtMost(50) - penalizacionLongitud

        val huecos = huecosDeSubsecuencia(consulta, etiqueta)
        if (huecos >= 0) return 400 - huecos.coerceAtMost(100) - penalizacionLongitud

        return SIN_COINCIDENCIA
    }

    /**
     * Cuenta los "saltos" necesarios para encontrar la consulta como subsecuencia de la etiqueta.
     * Menos huecos = coincidencia mas compacta = mejor.
     *
     * @return cantidad de caracteres saltados, o -1 si no es subsecuencia.
     */
    private fun huecosDeSubsecuencia(consulta: String, etiqueta: String): Int {
        var indiceEtiqueta = 0
        var huecos = 0
        for (caracter in consulta) {
            val encontrado = etiqueta.indexOf(caracter, startIndex = indiceEtiqueta)
            if (encontrado < 0) return -1
            huecos += encontrado - indiceEtiqueta
            indiceEtiqueta = encontrado + 1
        }
        return huecos
    }
}
