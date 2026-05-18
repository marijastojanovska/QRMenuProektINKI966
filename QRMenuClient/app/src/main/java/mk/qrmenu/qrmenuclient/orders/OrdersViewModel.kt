package mk.qrmenu.qrmenuclient.orders

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import mk.qrmenu.qrmenuclient.data.ClientIdProvider
import mk.qrmenu.qrmenuclient.data.OrderRepository
import mk.qrmenu.qrmenuclient.model.Order
import mk.qrmenu.qrmenuclient.model.OrderStatus

class OrdersViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val orderRepository = OrderRepository()
    private val clientId: String = ClientIdProvider.get(application)

    private val _statusFilter = MutableStateFlow<OrderStatus?>(null)
    val statusFilter: StateFlow<OrderStatus?> = _statusFilter.asStateFlow()

    val orders: StateFlow<List<Order>> = combine(
        orderRepository.observeClientOrders(clientId),
        _statusFilter,
    ) { list, filter ->
        if (filter == null) list
        else list.filter { OrderStatus.fromStorage(it.status) == filter }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setStatusFilter(status: OrderStatus?) {
        _statusFilter.value = status
    }
}
