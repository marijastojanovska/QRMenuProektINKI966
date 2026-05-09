package mk.qrmenu.qrmenumanager.main.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import mk.qrmenu.qrmenumanager.model.Product

class MenuViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val uid: String? = FirebaseAuth.getInstance().currentUser?.uid

    val products: StateFlow<List<Product>> = observeProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun observeProducts(): Flow<List<Product>> {
        val currentUid = uid ?: return flowOf(emptyList())
        return callbackFlow {
            val registration = firestore.collection("users")
                .document(currentUid)
                .collection("items")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        trySend(snapshot.toObjects(Product::class.java))
                    }
                }
            awaitClose { registration.remove() }
        }
    }
}
