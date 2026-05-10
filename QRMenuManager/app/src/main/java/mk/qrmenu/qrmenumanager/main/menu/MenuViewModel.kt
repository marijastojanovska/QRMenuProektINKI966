package mk.qrmenu.qrmenumanager.main.menu

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
import mk.qrmenu.qrmenumanager.model.Category
import mk.qrmenu.qrmenumanager.model.Product

class MenuViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val uid: String? = FirebaseAuth.getInstance().currentUser?.uid

    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory: StateFlow<Category?> = _selectedCategory.asStateFlow()

    val products: StateFlow<List<Product>> = combine(
        observeProducts(),
        _selectedCategory,
    ) { list, filter ->

        val filtered = if (filter == null) {
            list
        } else {
            list.filter { Category.fromStorage(it.category) == filter }
        }

        filtered.sortedWith(
            compareBy<Product> { Category.fromStorage(it.category)?.ordinal ?: Int.MAX_VALUE }
                .thenByDescending { it.createdAt?.time ?: 0L }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setCategoryFilter(category: Category?) {
        _selectedCategory.value = category
    }

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
