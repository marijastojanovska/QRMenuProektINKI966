package mk.qrmenu.qrmenumanager.main.addedit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import mk.qrmenu.qrmenumanager.R
import mk.qrmenu.qrmenumanager.model.Category
import mk.qrmenu.qrmenumanager.model.Product

data class FormState(
    val title: String = "",
    val description: String = "",
    val priceText: String = "",
    val imageUrl: String = "",
    val category: Category? = null,
    val isLoading: Boolean = false,
    val isEdit: Boolean = false,
    val loadedFromRemote: Boolean = false,
)

data class ValidationErrors(
    val titleErr: Int? = null,
    val priceErr: Int? = null,
    val imageErr: Int? = null,
    val categoryErr: Int? = null,
) {
    val hasError: Boolean
        get() = titleErr != null || priceErr != null || imageErr != null || categoryErr != null
}

sealed interface UiEvent {
    object NavigateBack : UiEvent
    data class ShowMessage(val message: String) : UiEvent
}

class AddEditProductViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _form = MutableStateFlow(FormState())
    val form: StateFlow<FormState> = _form.asStateFlow()

    private val _errors = MutableStateFlow(ValidationErrors())
    val errors: StateFlow<ValidationErrors> = _errors.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    private var editingId: String? = null

    fun setEditMode(productId: String?) {
        editingId = productId

        _form.update { it.copy(isEdit = productId != null) }

        if (productId != null && !_form.value.loadedFromRemote) {
            loadForEdit(productId)
        }
    }

    private fun loadForEdit(id: String) {
        val uid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            _form.update { it.copy(isLoading = true) }

            try {
                val snapshot = firestore.collection("users")
                    .document(uid)
                    .collection("items")
                    .document(id)
                    .get()
                    .await()

                val product = snapshot.toObject(Product::class.java)

                if (product != null) {
                    _form.update {
                        it.copy(
                            title = product.title,
                            description = product.description,
                            priceText = if (product.price > 0.0) product.price.toString() else "",
                            imageUrl = product.imageUrl,
                            category = Category.fromStorage(product.category),
                            isLoading = false,
                            loadedFromRemote = true,
                        )
                    }
                } else {
                    _form.update { it.copy(isLoading = false) }
                    _events.tryEmit(UiEvent.ShowMessage(string(R.string.error_product_not_found)))
                }
            } catch (t: Throwable) {
                _form.update { it.copy(isLoading = false) }
                _events.tryEmit(
                    UiEvent.ShowMessage(t.localizedMessage ?: string(R.string.error_load_failed))
                )
            }
        }
    }

    fun onTitleChanged(value: String) {
        _form.update { it.copy(title = value) }
    }

    fun onDescriptionChanged(value: String) {
        _form.update { it.copy(description = value) }
    }

    fun onPriceChanged(value: String) {
        _form.update { it.copy(priceText = value) }
    }

    fun onImageUrlChanged(value: String) {
        _form.update { it.copy(imageUrl = value) }

        if (value.isNotBlank()) {
            _errors.update { it.copy(imageErr = null) }
        }
    }

    fun onCategoryChanged(category: Category) {
        _form.update { it.copy(category = category) }
        _errors.update { it.copy(categoryErr = null) }
    }

    fun save() {
        val state = _form.value

        val uid = auth.currentUser?.uid ?: run {
            _events.tryEmit(UiEvent.ShowMessage(string(R.string.error_not_signed_in)))
            return
        }

        val price = state.priceText.toDoubleOrNull()
        val trimmedUrl = state.imageUrl.trim()
        val errors = ValidationErrors(
            titleErr = if (state.title.isBlank()) R.string.error_title_required else null,
            priceErr = if (price == null || price <= 0.0) R.string.error_price_invalid else null,
            imageErr = if (trimmedUrl.isBlank()) R.string.error_image_required else null,
            categoryErr = if (state.category == null) R.string.error_category_required else null,
        )

        _errors.value = errors

        if (errors.hasError) return

        viewModelScope.launch {
            _form.update { it.copy(isLoading = true) }

            try {
                val collection = firestore.collection("users")
                    .document(uid)
                    .collection("items")
                val docId = editingId ?: collection.document().id

                val product = Product(
                    id = docId,
                    title = state.title.trim(),
                    description = state.description.trim(),
                    price = price!!,
                    imageUrl = trimmedUrl,
                    category = state.category!!.name,
                )

                collection.document(docId).set(product).await()

                _form.update { it.copy(isLoading = false) }
                _events.tryEmit(UiEvent.NavigateBack)
            } catch (t: Throwable) {
                _form.update { it.copy(isLoading = false) }
                _events.tryEmit(
                    UiEvent.ShowMessage(t.localizedMessage ?: string(R.string.error_save_failed))
                )
            }
        }
    }

    fun delete() {
        val id = editingId ?: return

        val uid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            _form.update { it.copy(isLoading = true) }
            try {
                firestore.collection("users")
                    .document(uid)
                    .collection("items")
                    .document(id)
                    .delete()
                    .await()
                _form.update { it.copy(isLoading = false) }
                _events.tryEmit(UiEvent.NavigateBack)
            } catch (t: Throwable) {
                _form.update { it.copy(isLoading = false) }
                _events.tryEmit(
                    UiEvent.ShowMessage(t.localizedMessage ?: string(R.string.error_delete_failed))
                )
            }
        }
    }

    private fun string(resId: Int): String = getApplication<Application>().getString(resId)
}
