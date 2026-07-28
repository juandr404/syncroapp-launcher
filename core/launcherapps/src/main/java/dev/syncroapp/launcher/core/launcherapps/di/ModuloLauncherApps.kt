package dev.syncroapp.launcher.core.launcherapps.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.syncroapp.launcher.core.launcherapps.FuenteApps
import dev.syncroapp.launcher.core.launcherapps.FuenteAppsSistema
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ModuloLauncherApps {

    /**
     * La app entera depende de la interfaz [FuenteApps]; solo este binding conoce la
     * implementacion real. En tests se sustituye por un fake sin tocar nada mas.
     */
    @Binds
    @Singleton
    abstract fun enlazarFuenteApps(implementacion: FuenteAppsSistema): FuenteApps
}
