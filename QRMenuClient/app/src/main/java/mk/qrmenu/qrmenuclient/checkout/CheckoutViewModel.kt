package mk.qrmenu.qrmenuclient.checkout

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mk.qrmenu.qrmenuclient.cart.CartRepository
import mk.qrmenu.qrmenuclient.data.ClientIdProvider
import mk.qrmenu.qrmenuclient.data.OrderRepository
import mk.qrmenu.qrmenuclient.model.PaymentMethod

class CheckoutViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val orderRepository = OrderRepository()

    val cartState: StateFlow<mk.qrmenu.qrmenuclient.cart.CartState> = CartRepository.state

    private val _state = MutableStateFlow<CheckoutUiState>(CheckoutUiState.Idle)
    val state: StateFlow<CheckoutUiState> = _state.asStateFlow()

    fun submit(
        address: String,
        city: String,
        phone: String,
        paymentMethod: PaymentMethod,
    ) {
        val current = cartState.value
        val managerId = current.managerId
        if (managerId.isNullOrBlank() || current.entries.isEmpty()) return
        val ongoing = _state.value
        if (ongoing is CheckoutUiState.ProcessingPayment || ongoing is CheckoutUiState.Submitting) return

        val clientId = ClientIdProvider.get(getApplication())

        viewModelScope.launch {
            try {
                if (paymentMethod == PaymentMethod.CARD) {
                    _state.value = CheckoutUiState.ProcessingPayment
                    delay(PAYMENT_SIMULATION_MS)
                }
                _state.value = CheckoutUiState.Submitting

                val orderId = orderRepository.placeOrder(
                    clientId = clientId,
                    managerId = managerId,
                    items = current.entries,
                    customerAddress = address,
                    customerCity = city,
                    customerPhone = phone,
                    paymentMethod = paymentMethod,
                )
                CartRepository.clear()
                _state.value = CheckoutUiState.Success(orderId)
            } catch (t: Throwable) {
                _state.value = CheckoutUiState.Error(
                    t.localizedMessage ?: "Could not place order"
                )
            }
        }
    }

    fun consumeState() {
        _state.value = CheckoutUiState.Idle
    }

    private companion object {
        const val PAYMENT_SIMULATION_MS = 2000L
    }
}
