package dev.syncroapp.launcher.core.launcherapps

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.syncroapp.launcher.core.data.busqueda.PuntuadorBusqueda
import dev.syncroapp.launcher.core.data.di.ScopeAplicacion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementacion real de [FuenteApps] sobre LauncherApps.
 *
 * Se usa LauncherApps y no PackageManager por tres razones (ver ADR-006):
 *  1. Es la unica API que ve las apps del perfil de trabajo con su UserHandle correcto.
 *  2. Los launchers (intent-filter HOME) quedan exentos del filtrado de visibilidad de
 *     paquetes de Android 11+, asi que no hace falta pedir QUERY_ALL_PACKAGES.
 *  3. Su Callback avisa de instalaciones, desinstalaciones y cambios de perfil con
 *     mas precision que los broadcasts de paquete.
 */
@Singleton
class FuenteAppsSistema @Inject constructor(
    @ApplicationContext private val contexto: Context,
    @ScopeAplicacion private val scope: CoroutineScope,
) : FuenteApps {

    private val launcherApps =
        contexto.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

    private val userManager =
        contexto.getSystemService(Context.USER_SERVICE) as UserManager

    private val _apps = MutableStateFlow<List<AplicacionInstalada>>(emptyList())
    override val apps: StateFlow<List<AplicacionInstalada>> = _apps.asStateFlow()

    private val _cargaInicialCompletada = MutableStateFlow(false)
    override val cargaInicialCompletada: StateFlow<Boolean> = _cargaInicialCompletada.asStateFlow()

    /**
     * Handles de actividad indexados por clave estable, para poder pedir el icono despues.
     *
     * Se guardan los handles y no los Drawable: un LauncherActivityInfo pesa poco, mientras que
     * doscientos iconos rasterizados serian decenas de MB retenidos de forma permanente.
     */
    private val actividadesPorClave = java.util.concurrent.ConcurrentHashMap<String, LauncherActivityInfo>()

    /**
     * El callback se registra una sola vez y vive tanto como el proceso.
     * Cualquier evento invalida la cache y re-emite: la UI se actualiza sola.
     */
    private val callback = object : LauncherApps.Callback() {
        override fun onPackageAdded(packageName: String?, user: UserHandle?) = refrescar()
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) = refrescar()
        override fun onPackageChanged(packageName: String?, user: UserHandle?) = refrescar()

        override fun onPackagesAvailable(
            packageNames: Array<out String>?,
            user: UserHandle?,
            replacing: Boolean,
        ) = refrescar()

        override fun onPackagesUnavailable(
            packageNames: Array<out String>?,
            user: UserHandle?,
            replacing: Boolean,
        ) = refrescar()

        override fun onPackagesSuspended(packageNames: Array<out String>?, user: UserHandle?) =
            refrescar()

        override fun onPackagesUnsuspended(packageNames: Array<out String>?, user: UserHandle?) =
            refrescar()
    }

    init {
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        refrescar()
    }

    override fun refrescar() {
        scope.launch(Dispatchers.Default) {
            _apps.value = leerAppsInstaladas()
            _cargaInicialCompletada.value = true
        }
    }

    /**
     * Lee todas las apps lanzables de todos los perfiles del usuario.
     * Corre fuera del hilo principal: en un telefono con muchas apps tarda decenas de ms.
     */
    private fun leerAppsInstaladas(): List<AplicacionInstalada> {
        val perfilPersonal = Process.myUserHandle()
        actividadesPorClave.clear()

        return userManager.userProfiles.flatMap { perfil ->
            val serial = userManager.getSerialNumberForUser(perfil)
            val esTrabajo = perfil != perfilPersonal

            runCatching { launcherApps.getActivityList(null, perfil) }
                .onFailure { Log.w(TAG, "No se pudo leer el perfil $serial", it) }
                .getOrDefault(emptyList())
                .map { actividad ->
                    val etiqueta = actividad.label?.toString().orEmpty()
                    AplicacionInstalada(
                        paquete = actividad.applicationInfo.packageName,
                        clase = actividad.componentName.className,
                        serialUsuario = serial,
                        etiqueta = etiqueta,
                        etiquetaNormalizada = PuntuadorBusqueda.normalizar(etiqueta),
                        esPerfilTrabajo = esTrabajo,
                    ).also { app -> actividadesPorClave[app.claveEstable] = actividad }
                }
        }.sortedBy { it.etiquetaNormalizada }
    }

    override fun lanzar(app: AplicacionInstalada): Boolean = try {
        launcherApps.startMainActivity(
            ComponentName(app.paquete, app.clase),
            usuarioDe(app.serialUsuario),
            null,
            null,
        )
        true
    } catch (e: Exception) {
        // La app pudo desinstalarse entre el dibujo de la lista y el toque del usuario.
        Log.w(TAG, "No se pudo lanzar ${app.claveEstable}", e)
        refrescar()
        false
    }

    override fun abrirInfo(app: AplicacionInstalada) {
        runCatching {
            launcherApps.startAppDetailsActivity(
                ComponentName(app.paquete, app.clase),
                usuarioDe(app.serialUsuario),
                null,
                null,
            )
        }.onFailure { Log.w(TAG, "No se pudo abrir info de ${app.claveEstable}", it) }
    }

    override fun desinstalar(app: AplicacionInstalada) {
        runCatching {
            val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.paquete}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            contexto.startActivity(intent)
        }.onFailure { Log.w(TAG, "No se pudo desinstalar ${app.paquete}", it) }
    }

    override fun iconoDe(app: AplicacionInstalada): Drawable? {
        // Si la lista completa todavia no se ha leido, se consulta solo este componente.
        // Es una unica llamada al sistema: permite que los favoritos del inicio muestren su
        // icono de inmediato en vez de esperar a que se enumeren todas las apps instaladas.
        val actividad = actividadesPorClave[app.claveEstable] ?: buscarActividad(app) ?: return null
        // getBadgedIcon(0) usa la densidad de la pantalla y agrega la insignia del perfil
        // de trabajo cuando corresponde, sin que tengamos que dibujarla nosotros.
        return runCatching { actividad.getBadgedIcon(0) }.getOrNull()
    }

    private fun buscarActividad(app: AplicacionInstalada): LauncherActivityInfo? = runCatching {
        launcherApps
            .getActivityList(app.paquete, usuarioDe(app.serialUsuario))
            .firstOrNull { it.componentName.className == app.clase }
            ?.also { actividadesPorClave[app.claveEstable] = it }
    }.getOrNull()

    /** Traduce el serial persistido al UserHandle vivo; si el perfil ya no existe, usa el personal. */
    private fun usuarioDe(serial: Long): UserHandle =
        userManager.getUserForSerialNumber(serial) ?: Process.myUserHandle()

    private companion object {
        const val TAG = "FuenteAppsSistema"
    }
}
