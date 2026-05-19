package mk.qrmenu.qrmenuclient.model

import androidx.annotation.StringRes
import com.google.firebase.firestore.DocumentId
import mk.qrmenu.qrmenuclient.R

enum class Category(
    val firestoreValue: String,
    @StringRes val displayNameRes: Int,
    val sortOrder: Int,
) {
    COFFEE("coffee", R.string.category_coffee, 0),
    DRINKS("drinks", R.string.category_drinks, 1),
    FOOD("food", R.string.category_food, 2);

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
