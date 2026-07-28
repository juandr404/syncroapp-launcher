package dev.syncroapp.launcher.core.launcherapps

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestiona el estado de "launcher predeterminado".
 *
 * Ser el launcher activo no es cosmetico: mientras lo somos, el sistema mantiene vivo el
 * proceso y lo resucita si muere. Toda la fricción con los OEM agresivos se concentra en el
 * estado "instalado pero no predeterminado", por eso conviene salir de ese estado rapido.
 */
@Singleton
class GestorLauncherPredeterminado @Inject constructor(
    @ApplicationContext private val contexto: Context,
) {

    /** true si SyncroApp Launcher es el launcher activo del sistema. */
    fun esPredeterminado(): Boolean {
        val intentHome = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolucion = contexto.packageManager
            .resolveActivity(intentHome, PackageManager.MATCH_DEFAULT_ONLY)
        return resolucion?.activityInfo?.packageName == contexto.packageName
    }

    /**
     * Intent para pedirle al usuario que nos ponga como launcher.
     *
     * En Android 10+ es el dialogo del sistema (RoleManager). En versiones previas —y en capas
     * como MIUI donde el dialogo a veces no aparece— se cae a la pantalla de ajustes de inicio,
     * que siempre existe. Por eso la UI ofrece ademas un boton explicito de "abrir ajustes".
     */
    fun intentParaElegir(): Intent {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = contexto.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                return roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
            }
        }
        return intentAjustesDeInicio()
    }

    /** Pantalla del sistema donde se elige la app de inicio. Plan B universal. */
    fun intentAjustesDeInicio(): Intent =
        Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
