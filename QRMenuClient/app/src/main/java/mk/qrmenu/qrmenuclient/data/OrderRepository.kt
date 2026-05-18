package mk.qrmenu.qrmenuclient.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import mk.qrmenu.qrmenuclient.model.Order
import mk.qrmenu.qrmenuclient.model.OrderItem
import mk.qrmenu.qrmenuclient.model.OrderStatus

class OrderRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {

    suspend fun placeOrder(
        clientId: String,
        managerId: String,
        items: List<OrderItem>,
    ): String {
        val managerCol = managerOrdersRef(managerId)
        val clientCol = clientOrdersRef(clientId)

        val docId = managerCol.document().id
        val managerRef = managerCol.document(docId)
        val clientRef = clientCol.document(docId)

        val order = Order(
            clientId = clientId,
            managerId = managerId,
            status = OrderStatus.PENDING.name,
            items = items,
        )

        firestore.batch().apply {
            set(managerRef, order)
            set(clientRef, order)
        }.commit().await()

        return docId
    }

    fun observeClientOrders(clientId: String): Flow<List<Order>> = callbackFlow {
        val registration = clientOrdersRef(clientId)
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

    private fun managerOrdersRef(managerId: String) =
        firestore.collection("orders")
            .document("manager")
            .collection(managerId)

    private fun clientOrdersRef(clientId: String) =
        firestore.collection("orders")
            .document("client")
            .collection(clientId)
}
