package mk.qrmenu.qrmenumanager.model

import androidx.annotation.StringRes
import mk.qrmenu.qrmenumanager.R

enum class Category(@StringRes val labelRes: Int) {
    COFFEE(R.string.category_coffee),
    DRINKS(R.string.category_drinks),
    FOOD(R.string.category_food);

    companion object {
        fun fromStorage(value: String?): Category? {
            if (value.isNullOrBlank()) return null
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
        }
    }
}
