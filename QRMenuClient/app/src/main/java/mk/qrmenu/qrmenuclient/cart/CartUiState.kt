package mk.qrmenu.qrmenuclient.cart

sealed interface CartSubmitState {
    data object Idle : CartSubmitState
    data object Submitting : CartSubmitState
    data class Success(val orderId: String) : CartSubmitState
    data class Error(val message: String) : CartSubmitState
}
