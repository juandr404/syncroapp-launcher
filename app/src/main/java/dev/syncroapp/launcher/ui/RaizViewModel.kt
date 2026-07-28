package dev.syncroapp.launcher.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.syncroapp.launcher.core.data.ajustes.RepositorioAjustes
import dev.syncroapp.launcher.core.data.modelo.AjustesLauncher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Solo expone la configuracion para que la raiz pueda aplicar el tema. */
@HiltViewModel
class RaizViewModel @Inject constructor(
    repositorioAjustes: RepositorioAjustes,
) : ViewModel() {

    val ajustes: StateFlow<AjustesLauncher> = repositorioAjustes.ajustes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(TIEMPO_VIVO_MS),
            initialValue = AjustesLauncher(),
        )

    private companion object {
        const val TIEMPO_VIVO_MS = 5_000L
    }
}
