package mk.qrmenu.qrmenuclient.cart

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.StateFlow

class CartViewModel(
    application: Application,
) : AndroidViewModel(application) {

    val cartState: StateFlow<CartState> = CartRepository.state

    fun increment(productId: String) {
        CartRepository.increment(productId)
    }

    fun decrement(productId: String) {
        CartRepository.decrement(productId)
    }

    fun remove(productId: String) {
        CartRepository.remove(productId)
    }
}
