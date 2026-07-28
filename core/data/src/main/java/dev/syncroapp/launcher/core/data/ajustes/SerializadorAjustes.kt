package dev.syncroapp.launcher.core.data.ajustes

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import dev.syncroapp.launcher.core.data.modelo.AjustesLauncher
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

/**
 * Serializador de [AjustesLauncher] para DataStore.
 *
 * Se usa JSON en vez de Protobuf porque da el mismo esquema tipado y los mismos defaults
 * en un solo lugar, sin agregar el paso de codegen de protoc al build (ver ADR-007).
 *
 * - ignoreUnknownKeys: un archivo escrito por una version futura no rompe una version vieja.
 * - encodeDefaults: el archivo siempre queda completo y legible a ojo para depurar.
 */
object SerializadorAjustes : Serializer<AjustesLauncher> {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    override val defaultValue: AjustesLauncher = AjustesLauncher()

    override suspend fun readFrom(input: InputStream): AjustesLauncher =
        try {
            json.decodeFromString(
                AjustesLauncher.serializer(),
                input.readBytes().decodeToString(),
            )
        } catch (e: SerializationException) {
            // DataStore captura CorruptionException y aplica el handler de recuperacion.
            throw CorruptionException("Archivo de ajustes corrupto", e)
        }

    override suspend fun writeTo(t: AjustesLauncher, output: OutputStream) {
        output.write(
            json.encodeToString(AjustesLauncher.serializer(), t).encodeToByteArray(),
        )
    }
}
