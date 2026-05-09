package mk.qrmenu.qrmenuclient.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import mk.qrmenu.qrmenuclient.model.Product

class MenuRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    suspend fun getMenu(userId: String): List<Product> {
        return firestore.collection("users")
            .document(userId)
            .collection("items")
            .orderBy("title", Query.Direction.ASCENDING)
            .get()
            .await()
            .toObjects(Product::class.java)
    }
}
