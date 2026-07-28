package dev.syncroapp.launcher.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.syncroapp.launcher.core.data.ajustes.SerializadorAjustes
import dev.syncroapp.launcher.core.data.modelo.AjustesLauncher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/** Scope de aplicacion: vive mientras viva el proceso del launcher. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ScopeAplicacion

@Module
@InstallIn(SingletonComponent::class)
object ModuloDatos {

    @Provides
    @Singleton
    @ScopeAplicacion
    fun proveerScopeAplicacion(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun proveerDataStoreAjustes(
        @ApplicationContext contexto: Context,
        @ScopeAplicacion scope: CoroutineScope,
    ): DataStore<AjustesLauncher> = DataStoreFactory.create(
        serializer = SerializadorAjustes,
        // Si el archivo se corrompe, se recupera con los defaults en vez de crashear el launcher:
        // quedarse sin pantalla de inicio es el peor fallo posible de este producto.
        corruptionHandler = ReplaceFileCorruptionHandler { AjustesLauncher() },
        scope = scope,
        produceFile = { contexto.dataStoreFile("ajustes.json") },
    )
}
