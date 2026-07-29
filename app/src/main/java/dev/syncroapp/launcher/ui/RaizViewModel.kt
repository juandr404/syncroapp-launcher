package dev.syncroapp.launcher.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.syncroapp.launcher.core.data.ajustes.RepositorioAjustes
import dev.syncroapp.launcher.core.data.modelo.AjustesLauncher
import dev.syncroapp.launcher.core.launcherapps.GestorLauncherPredeterminado
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Estado que necesita la raiz: el tema y si somos el launcher del sistema.
 *
 * Lo segundo determina el comportamiento del boton atras (ver [RaizLauncher]), por eso vive
 * aqui y no dentro de una pantalla.
 */
@HiltViewModel
class RaizViewModel @Inject constructor(
    repositorioAjustes: RepositorioAjustes,
    private val gestorPredeterminado: GestorLauncherPredeterminado,
) : ViewModel() {

    val ajustes: StateFlow<AjustesLauncher> = repositorioAjustes.ajustes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(TIEMPO_VIVO_MS),
            initialValue = AjustesLauncher(),
        )

    private val _esPredeterminado = MutableStateFlow(gestorPredeterminado.esPredeterminado())
    val esPredeterminado: StateFlow<Boolean> = _esPredeterminado.asStateFlow()

    fun revisarSiEsPredeterminado() {
        _esPredeterminado.value = gestorPredeterminado.esPredeterminado()
    }

    private companion object {
        const val TIEMPO_VIVO_MS = 5_000L
    }
}
