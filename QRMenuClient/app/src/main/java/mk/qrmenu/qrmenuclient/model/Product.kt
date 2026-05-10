package mk.qrmenu.qrmenuclient.model

import com.google.firebase.firestore.DocumentId

enum class Category(
    val firestoreValue: String,
    val displayName: String,
    val sortOrder: Int,
) {
    COFFEE("coffee", "Coffee", 0),
    DRINKS("drinks", "Drinks", 1),
    FOOD("food", "Food", 2);

    companion object {
        fun fromFirestore(value: String?): Category? {
            val v = value?.trim().orEmpty()
            if (v.isEmpty()) return null
            return values().firstOrNull { it.firestoreValue.equals(v, ignoreCase = true) }
        }
    }
}

data class Product(
    @DocumentId var id: String = "",
    var title: String = "",
    var description: String = "",
    var price: Double = 0.0,
    var imageUrl: String = "",
    var category: String = "",
) {
    val categoryEnum: Category?
        get() = Category.fromFirestore(category)
}
