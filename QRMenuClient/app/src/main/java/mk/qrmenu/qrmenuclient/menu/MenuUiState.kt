package mk.qrmenu.qrmenuclient.menu

import mk.qrmenu.qrmenuclient.model.Product

sealed interface MenuUiState {
    data object Idle : MenuUiState
    data object Loading : MenuUiState
    data class Success(val items: List<Product>) : MenuUiState
    data class Error(val message: String) : MenuUiState
}
