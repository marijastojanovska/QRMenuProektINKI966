package mk.qrmenu.qrmenuclient.cart

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mk.qrmenu.qrmenuclient.data.ClientIdProvider
import mk.qrmenu.qrmenuclient.data.OrderRepository

class CartViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val orderRepository = OrderRepository()

    val cartState: StateFlow<CartState> = CartRepository.state

    private val _submitState = MutableStateFlow<CartSubmitState>(CartSubmitState.Idle)
    val submitState: StateFlow<CartSubmitState> = _submitState.asStateFlow()

    fun increment(productId: String) {
        CartRepository.increment(productId)
    }

    fun decrement(productId: String) {
        CartRepository.decrement(productId)
    }

    fun remove(productId: String) {
        CartRepository.remove(productId)
    }

    fun placeOrder() {
        val current = cartState.value
        val managerId = current.managerId
        if (managerId.isNullOrBlank() || current.entries.isEmpty()) return
        if (_submitState.value is CartSubmitState.Submitting) return

        _submitState.value = CartSubmitState.Submitting

        val clientId = ClientIdProvider.get(getApplication())

        viewModelScope.launch {
            try {
                val orderId = orderRepository.placeOrder(
                    clientId = clientId,
                    managerId = managerId,
                    items = current.entries,
                )
                CartRepository.clear()
                _submitState.value = CartSubmitState.Success(orderId)
            } catch (t: Throwable) {
                _submitState.value = CartSubmitState.Error(
                    t.localizedMessage ?: "Could not place order"
                )
            }
        }
    }

    fun consumeSubmitState() {
        _submitState.value = CartSubmitState.Idle
    }
}
