package dev.syncroapp.launcher.core.ui.componentes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.syncroapp.launcher.core.data.modelo.Alineacion
import dev.syncroapp.launcher.core.data.modelo.GrosorTrazo
import dev.syncroapp.launcher.core.data.modelo.TamanoDia
import dev.syncroapp.launcher.core.ui.tema.Espacio
import dev.syncroapp.launcher.core.ui.tema.TemaLauncher
import dev.syncroapp.launcher.core.ui.tema.anchoTrazoDe
import dev.syncroapp.launcher.core.ui.tema.rangoTamanoDia
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.time.format.TextStyle as EstiloTextoJava

/**
 * Bloque del reloj: dia de la semana en contorno gigante, con la hora y la fecha
 * flanqueandolo a la altura de su centro optico.
 *
 * El dia gigante es ornamento estructural: no tiene accion al tocarlo y para TalkBack es
 * decorativo, porque la misma informacion la dan la hora y la fecha, que si son legibles.
 */
@Composable
fun RelojGigante(
    instante: LocalDateTime,
    formato24h: Boolean,
    mostrarDiaGigante: Boolean,
    tamanoDia: TamanoDia,
    mostrarFecha: Boolean,
    grosorTrazo: GrosorTrazo,
    alineacion: Alineacion,
    diaEnIngles: Boolean,
    onTocarHora: () -> Unit,
    onTocarFecha: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = TemaLauncher.colores
    val tipografia = TemaLauncher.tipografia
    val local = if (diaEnIngles) Locale.ENGLISH else Locale.getDefault()

    val textoDia = remember(instante.dayOfWeek, local) {
        instante.dayOfWeek
            .getDisplayName(EstiloTextoJava.SHORT, local)
            .replace(".", "")
            .uppercase(local)
    }

    val textoHora = remember(instante.hour, instante.minute, formato24h) {
        val patron = if (formato24h) "HH:mm" else "h:mm a"
        instante.format(DateTimeFormatter.ofPattern(patron, Locale.getDefault()))
    }

    val textoFecha = remember(instante.dayOfMonth, instante.month, local) {
        instante.format(DateTimeFormatter.ofPattern("d MMM", local))
            .replace(".", "")
            .uppercase(local)
    }

    val descripcionFecha = remember(instante.dayOfMonth, instante.month) {
        instante.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.getDefault()))
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = when (alineacion) {
            Alineacion.IZQUIERDA -> Alignment.Start
            Alineacion.CENTRO -> Alignment.CenterHorizontally
            Alineacion.DERECHA -> Alignment.End
        },
    ) {
        if (mostrarDiaGigante) {
            // Hora y fecha en linea propia, alineadas a los extremos, ENCIMA del dia.
            //
            // El dia auto-escala hasta llenar el ancho, asi que siempre queda a sangre; superponer
            // la hora y la fecha sobre el depende de la letra que toque ese dia: sobre la M se lee
            // bien, sobre la R la diagonal la parte. Un reloj que se lee martes pero no miercoles
            // no es un reloj.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Espacio.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextoHora(textoHora, onTocarHora)
                if (mostrarFecha) TextoFecha(textoFecha, descripcionFecha, onTocarFecha)
            }

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val anchoDisponible = maxWidth
                val rango = rangoTamanoDia(tamanoDia)

                val estiloDia = TextStyle(
                    fontFamily = tipografia.familiaDisplay,
                    fontWeight = FontWeight.Black, // el contorno sobre peso alto no colapsa
                    letterSpacing = (-1).sp,
                    color = colores.textoPrimario,
                    drawStyle = Stroke(
                        width = with(LocalDensity.current) {
                            anchoTrazoDe(grosorTrazo, tamanoDia).toPx()
                        },
                        join = StrokeJoin.Round, // sin picos en los vertices de la W y la N
                        cap = StrokeCap.Round,
                    ),
                )

                val tamanoCalculado = tamanoQueCabe(
                    texto = textoDia,
                    estiloBase = estiloDia,
                    anchoMaximoDp = anchoDisponible,
                    minSp = rango.start,
                    maxSp = rango.endInclusive,
                )

                Text(
                    text = textoDia,
                    style = estiloDia.copy(fontSize = tamanoCalculado),
                    maxLines = 1,
                    softWrap = false,
                    // Cuando el dia no llena el ancho (tamano pequeno o mediano) tiene que
                    // seguir la alineacion global, o queda flotando sin relacion con la hora.
                    textAlign = when (alineacion) {
                        Alineacion.IZQUIERDA -> TextAlign.Start
                        Alineacion.CENTRO -> TextAlign.Center
                        Alineacion.DERECHA -> TextAlign.End
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        // Decorativo: su informacion ya la anuncian la hora y la fecha.
                        .clearAndSetSemantics { },
                )
            }
        } else {
            // Sin dia gigante: la hora toma el rol principal y la fecha va debajo.
            Text(
                text = textoHora,
                style = tipografia.hora.copy(fontSize = 64.sp, color = colores.textoPrimario),
                modifier = Modifier
                    .clickable(onClickLabel = "Abrir reloj", onClick = onTocarHora)
                    .semantics { contentDescription = "Hora $textoHora" },
            )
            if (mostrarFecha) {
                Box(modifier = Modifier.padding(top = Espacio.s)) {
                    TextoFecha(textoFecha, descripcionFecha, onTocarFecha)
                }
            }
        }
    }
}

@Composable
private fun TextoHora(texto: String, onTocar: () -> Unit) {
    val colores = TemaLauncher.colores
    Text(
        text = texto,
        style = TemaLauncher.tipografia.hora.copy(color = colores.textoPrimario),
        modifier = Modifier
            .clickable(onClickLabel = "Abrir reloj", onClick = onTocar)
            .padding(vertical = 8.dp, horizontal = 2.dp)
            .semantics { contentDescription = "Hora $texto" },
    )
}

@Composable
private fun TextoFecha(texto: String, descripcion: String, onTocar: () -> Unit) {
    val colores = TemaLauncher.colores
    Text(
        text = texto,
        style = TemaLauncher.tipografia.fecha.copy(color = colores.textoSecundario),
        modifier = Modifier
            .clickable(onClickLabel = "Abrir calendario", onClick = onTocar)
            .padding(vertical = 8.dp, horizontal = 2.dp)
            .semantics { contentDescription = descripcion },
    )
}

/**
 * Calcula el mayor tamano de fuente con el que [texto] cabe en una sola linea.
 *
 * Se mide de verdad con el motor de texto en vez de estimar por cantidad de caracteres:
 * "MIE" y "WED" ocupan distinto, y el usuario puede tener escalado de fuente al 200%.
 */
@Composable
private fun tamanoQueCabe(
    texto: String,
    estiloBase: TextStyle,
    anchoMaximoDp: androidx.compose.ui.unit.Dp,
    minSp: Float,
    maxSp: Float,
): TextUnit {
    val medidor = rememberTextMeasurer()
    val densidad = LocalDensity.current
    val anchoMaximoPx = with(densidad) { (anchoMaximoDp * FRACCION_ANCHO_UTIL).toPx() }

    return remember(texto, anchoMaximoPx, minSp, maxSp, estiloBase.fontFamily) {
        var candidato = maxSp
        while (candidato > minSp) {
            val ancho = medidor.measure(
                text = texto,
                style = estiloBase.copy(fontSize = candidato.sp),
                maxLines = 1,
                softWrap = false,
            ).size.width
            if (ancho <= anchoMaximoPx) break
            candidato -= PASO_REDUCCION_SP
        }
        candidato.coerceAtLeast(minSp).sp
    }
}

/** El dia ocupa como maximo el 96% del ancho util: necesita aire a los lados. */
private const val FRACCION_ANCHO_UTIL = 0.96f
private const val PASO_REDUCCION_SP = 2f
