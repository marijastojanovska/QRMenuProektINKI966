package mk.qrmenu.qrmenumanager.main.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mk.qrmenu.qrmenumanager.model.Order
import mk.qrmenu.qrmenumanager.model.OrderStatus

class OrdersViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val uid: String? = FirebaseAuth.getInstance().currentUser?.uid

    private val _statusFilter = MutableStateFlow<OrderStatus?>(null)
    val statusFilter: StateFlow<OrderStatus?> = _statusFilter.asStateFlow()

    val orders: StateFlow<List<Order>> = combine(
        observeOrders(),
        _statusFilter,
    ) { list, filter ->
        if (filter == null) list
        else list.filter { OrderStatus.fromStorage(it.status) == filter }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setStatusFilter(status: OrderStatus?) {
        _statusFilter.value = status
    }

    fun setStatus(order: Order, newStatus: OrderStatus) {
        val managerId = uid ?: return
        if (order.id.isBlank() || order.clientId.isBlank()) return

        val managerRef = managerOrdersRef(managerId).document(order.id)
        val clientRef = clientOrdersRef(order.clientId).document(order.id)

        viewModelScope.launch {
            firestore.batch().apply {
                update(managerRef, "status", newStatus.name)
                update(clientRef, "status", newStatus.name)
            }.commit()
        }
    }

    private fun observeOrders(): Flow<List<Order>> {
        val currentUid = uid ?: return flowOf(emptyList())

        return callbackFlow {
            val registration = managerOrdersRef(currentUid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        trySend(snapshot.toObjects(Order::class.java))
                    }
                }
            awaitClose { registration.remove() }
        }
    }

    private fun managerOrdersRef(managerId: String) =
        firestore.collection("orders")
            .document("manager")
            .collection(managerId)

    private fun clientOrdersRef(clientId: String) =
        firestore.collection("orders")
            .document("client")
            .collection(clientId)
}
