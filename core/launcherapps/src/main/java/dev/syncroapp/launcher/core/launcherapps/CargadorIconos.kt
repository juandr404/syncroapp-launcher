package dev.syncroapp.launcher.core.launcherapps

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.LruCache
import javax.inject.Inject
import javax.inject.Singleton

/** Como se dibuja el icono de una app en la lista. */
enum class EstiloIcono { MONOCROMO, ORIGINAL }

/**
 * Convierte los iconos del sistema en bitmaps listos para dibujar, con cache en memoria.
 *
 * El trabajo real (rasterizar un drawable, desaturarlo) cuesta milisegundos por icono; hacerlo
 * en cada recomposicion de una lista de 200 apps se nota. La cache lo hace una sola vez por
 * combinacion de app, estilo y tamano.
 */
@Singleton
class CargadorIconos @Inject constructor(
    private val fuenteApps: FuenteApps,
) {

    /**
     * Presupuesto deliberadamente pequeno: un launcher vive residente en memoria y cada MB que
     * retiene es un MB que le falta al resto del telefono para siempre.
     */
    private val cache = object : LruCache<String, Bitmap>(MAX_ICONOS_EN_CACHE) {
        override fun sizeOf(key: String, value: Bitmap): Int = 1
    }

    fun cargar(
        app: AplicacionInstalada,
        estilo: EstiloIcono,
        tamanoPx: Int,
        colorTinte: Int,
    ): Bitmap? {
        if (tamanoPx <= 0) return null

        val clave = "${app.claveEstable}|$estilo|$tamanoPx|$colorTinte"
        cache.get(clave)?.let { return it }

        val original = fuenteApps.iconoDe(app) ?: return null
        val bitmap = when (estilo) {
            EstiloIcono.ORIGINAL -> rasterizar(original, tamanoPx)
            EstiloIcono.MONOCROMO -> rasterizarMonocromo(original, tamanoPx, colorTinte)
        }

        cache.put(clave, bitmap)
        return bitmap
    }

    /** Libera la cache cuando el sistema avisa que necesita memoria. */
    fun vaciarCache() = cache.evictAll()

    private fun rasterizar(drawable: Drawable, tamanoPx: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(tamanoPx, tamanoPx, Bitmap.Config.ARGB_8888)
        val lienzo = Canvas(bitmap)
        drawable.setBounds(0, 0, tamanoPx, tamanoPx)
        drawable.draw(lienzo)
        return bitmap
    }

    /**
     * Version monocroma del icono.
     *
     * Si la app trae la capa `monochrome` de los iconos tematizados de Android 13+, se usa esa:
     * es la silueta que diseno el propio autor de la app, tintada con el color del texto.
     *
     * Si no la trae, se desatura el icono. En los adaptativos se desatura SOLO la capa frontal:
     * la de fondo es una placa opaca que, pintada de un solo color, convierte cualquier icono
     * en un cuadrado sin forma (el bug de los "cuadrados blancos" de la primera version).
     */
    private fun rasterizarMonocromo(drawable: Drawable, tamanoPx: Int, colorTinte: Int): Bitmap {
        val adaptativo = drawable as? AdaptiveIconDrawable

        val capaMonocroma = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            adaptativo?.monochrome
        } else {
            null
        }

        val bitmap = Bitmap.createBitmap(tamanoPx, tamanoPx, Bitmap.Config.ARGB_8888)
        val lienzo = Canvas(bitmap)

        if (capaMonocroma != null) {
            val copia = capaMonocroma.mutate()
            expandirCapaAdaptativa(copia, tamanoPx)
            copia.setTint(colorTinte)
            copia.draw(lienzo)
            return bitmap
        }

        val fuente = (adaptativo?.foreground ?: drawable).mutate()
        if (adaptativo != null) expandirCapaAdaptativa(fuente, tamanoPx) else fuente.setBounds(0, 0, tamanoPx, tamanoPx)
        fuente.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
        fuente.draw(lienzo)
        fuente.clearColorFilter()
        return bitmap
    }

    /**
     * Las capas de un icono adaptativo se disenan sobre un lienzo de 108 dp del que solo es
     * visible el centro de 66 dp. Dibujarlas sin compensar deja el simbolo diminuto: hay que
     * sacar los bordes del lienzo fuera del bitmap.
     */
    private fun expandirCapaAdaptativa(capa: Drawable, tamanoPx: Int) {
        val margen = (tamanoPx * EXPANSION_CAPA_ADAPTATIVA).toInt()
        capa.setBounds(-margen, -margen, tamanoPx + margen, tamanoPx + margen)
    }

    private companion object {
        const val MAX_ICONOS_EN_CACHE = 220

        /** (108 - 66) / 2 / 66: mitad del borde no visible, relativo al area visible. */
        const val EXPANSION_CAPA_ADAPTATIVA = 0.318f
    }
}
