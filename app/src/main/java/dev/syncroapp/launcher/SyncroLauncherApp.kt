package dev.syncroapp.launcher

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Punto de entrada del proceso.
 *
 * No hace trabajo pesado aqui: el proceso del launcher arranca junto con el sistema y todo
 * lo que se inicialice de forma temprana se paga en el tiempo hasta ver la pantalla de inicio.
 */
@HiltAndroidApp
class SyncroLauncherApp : Application()
