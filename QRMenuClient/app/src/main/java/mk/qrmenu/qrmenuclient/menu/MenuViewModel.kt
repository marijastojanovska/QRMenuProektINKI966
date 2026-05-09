package mk.qrmenu.qrmenuclient.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mk.qrmenu.qrmenuclient.data.MenuRepository

class MenuViewModel(
    private val repository: MenuRepository = MenuRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow<MenuUiState>(MenuUiState.Idle)
    val uiState: StateFlow<MenuUiState> = _uiState.asStateFlow()

    private var lastUserId: String? = null

    fun loadMenu(userId: String) {
        val trimmed = userId.trim()
        if (trimmed.isEmpty()) {
            _uiState.value = MenuUiState.Error("Empty QR code")
            return
        }
        lastUserId = trimmed
        _uiState.value = MenuUiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                MenuUiState.Success(repository.getMenu(trimmed))
            } catch (t: Throwable) {
                MenuUiState.Error(t.localizedMessage ?: "Unknown error")
            }
        }
    }

    fun retry() {
        lastUserId?.let(::loadMenu)
    }
}
