package mk.qrmenu.qrmenuclient.checkout

sealed interface CheckoutUiState {
    data object Idle : CheckoutUiState
    data object ProcessingPayment : CheckoutUiState
    data object Submitting : CheckoutUiState
    data class Success(val orderId: String) : CheckoutUiState
    data class Error(val message: String) : CheckoutUiState
}
