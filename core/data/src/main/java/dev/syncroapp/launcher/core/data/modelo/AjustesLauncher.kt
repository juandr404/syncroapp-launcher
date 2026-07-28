package dev.syncroapp.launcher.core.data.modelo

import kotlinx.serialization.Serializable

/**
 * Alineacion global del contenido (reloj + listas).
 * Se aplica de forma solidaria: no se permite reloj centrado con lista a la izquierda,
 * porque fragmenta la reticula (ver docs de diseno, seccion 4.4).
 */
enum class Alineacion { IZQUIERDA, CENTRO, DERECHA }

/** Densidad de las filas de aplicaciones. Define alto de fila, tamano de texto e icono. */
enum class Densidad { COMPACTA, MEDIA, AMPLIA }

/** Temas soportados. DARK_PURO es el tema insignia (negro #000000, ahorro real en OLED). */
enum class Tema { DARK_PURO, DARK_SUAVE, CLARO, SEGUN_SISTEMA }

/** Grosor del trazo del dia gigante en outline. */
enum class GrosorTrazo { FINO, MEDIO, GRUESO }

/**
 * Tamano del dia de la semana.
 * GRANDE lo lleva a sangre de borde a borde; PEQUENO lo deja como un detalle junto a la hora.
 */
enum class TamanoDia { PEQUENO, MEDIANO, GRANDE }

/** Estilo de los iconos de la lista de apps. */
enum class EstiloIconos {
    /** Sin iconos: la lista es solo texto. Default del producto. */
    NINGUNO,

    /** Capa monochrome del icono adaptativo (Android 13+), tintada con el color de texto. */
    MONOCROMO,
}

/**
 * Un favorito guardado. Se referencia por componente + usuario (no por indice),
 * para que sobreviva a reordenamientos, reinstalaciones y perfiles de trabajo.
 *
 * @param alias nombre personalizado que reemplaza la etiqueta de la app (null = usar la original).
 */
@Serializable
data class FavoritoGuardado(
    val paquete: String,
    val clase: String,
    val serialUsuario: Long,
    val alias: String? = null,
)

/**
 * Estado completo de configuracion del launcher.
 *
 * Es un unico objeto tipado persistido con DataStore + kotlinx.serialization: los defaults
 * viven aqui, en un solo lugar, y agregar un ajuste nuevo no rompe los archivos existentes
 * (el Json se lee con ignoreUnknownKeys).
 */
@Serializable
data class AjustesLauncher(
    // --- Pantalla ---
    val tema: Tema = Tema.DARK_PURO,
    val alineacion: Alineacion = Alineacion.IZQUIERDA,
    val densidad: Densidad = Densidad.MEDIA,
    /** Texto primario en blanco puro (#FFFFFF) en vez del blanco suave que evita halo en OLED. */
    val blancoMaximo: Boolean = false,

    // --- Reloj ---
    val formato24h: Boolean = true,
    val mostrarDiaGigante: Boolean = true,
    val tamanoDia: TamanoDia = TamanoDia.MEDIANO,
    val mostrarFecha: Boolean = true,
    val grosorTrazo: GrosorTrazo = GrosorTrazo.MEDIO,
    /** true = el dia de la semana se muestra en ingles ("WED") ignorando el idioma del sistema. */
    val diaEnIngles: Boolean = false,

    // --- Apps ---
    val favoritos: List<FavoritoGuardado> = emptyList(),
    val estiloIconos: EstiloIconos = EstiloIconos.NINGUNO,
    /** Claves estables ("paquete/clase#serial") de apps ocultas del cajon y la busqueda. */
    val appsOcultas: Set<String> = emptySet(),

    // --- Busqueda ---
    val tecladoAutomatico: Boolean = true,
    /** Abrir la app automaticamente cuando la busqueda deja un unico resultado. */
    val abrirUnicoResultado: Boolean = false,

    // --- Onboarding ---
    val onboardingCompletado: Boolean = false,
) {
    /** Maximo de favoritos que se muestran en el Home (mas que esto deja de ser un Home). */
    companion object {
        const val MAX_FAVORITOS = 8
    }
}
