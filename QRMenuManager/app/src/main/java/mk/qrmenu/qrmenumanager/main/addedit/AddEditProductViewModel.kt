package mk.qrmenu.qrmenumanager.main.addedit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import mk.qrmenu.qrmenumanager.R
import mk.qrmenu.qrmenumanager.model.Product
import java.io.ByteArrayOutputStream
import kotlin.math.max

data class FormState(
    val title: String = "",
    val description: String = "",
    val priceText: String = "",
    val imageUrl: String = "",
    val localImageUri: Uri? = null,
    val isLoading: Boolean = false,
    val isEdit: Boolean = false,
    val loadedFromRemote: Boolean = false,
)

data class ValidationErrors(
    val titleErr: Int? = null,
    val priceErr: Int? = null,
    val imageErr: Int? = null,
) {
    val hasError: Boolean
        get() = titleErr != null || priceErr != null || imageErr != null
}

sealed interface UiEvent {
    object NavigateBack : UiEvent
    data class ShowMessage(val message: String) : UiEvent
}

class AddEditProductViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
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
                            isLoading = false,
                            loadedFromRemote = true,
                        )
                    }
                } else {
                    _form.update { it.copy(isLoading = false) }
                    _events.tryEmit(UiEvent.ShowMessage("Product not found"))
                }
            } catch (t: Throwable) {
                _form.update { it.copy(isLoading = false) }
                _events.tryEmit(UiEvent.ShowMessage(t.localizedMessage ?: "Failed to load"))
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

    fun onImagePicked(uri: Uri) {
        _form.update { it.copy(localImageUri = uri) }
        _errors.update { it.copy(imageErr = null) }
    }

    fun save(context: Context) {
        val state = _form.value
        val uid = auth.currentUser?.uid ?: run {
            _events.tryEmit(UiEvent.ShowMessage("Not signed in"))
            return
        }

        val price = state.priceText.toDoubleOrNull()
        val errors = ValidationErrors(
            titleErr = if (state.title.isBlank()) R.string.error_title_required else null,
            priceErr = if (price == null || price <= 0.0) R.string.error_price_invalid else null,
            imageErr = if (!state.isEdit && state.localImageUri == null) R.string.error_image_required else null,
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

                var imageUrl = state.imageUrl
                val localUri = state.localImageUri
                if (localUri != null) {
                    val bytes = withContext(Dispatchers.IO) {
                        compressImage(context, localUri)
                    }
                    val ref = storage.reference.child("users/$uid/items/$docId.jpg")
                    ref.putBytes(bytes).await()
                    imageUrl = ref.downloadUrl.await().toString()
                }

                val product = Product(
                    id = docId,
                    title = state.title.trim(),
                    description = state.description.trim(),
                    price = price!!,
                    imageUrl = imageUrl,
                )
                collection.document(docId).set(product).await()

                _form.update { it.copy(isLoading = false) }
                _events.tryEmit(UiEvent.NavigateBack)
            } catch (t: Throwable) {
                _form.update { it.copy(isLoading = false) }
                _events.tryEmit(UiEvent.ShowMessage(t.localizedMessage ?: "Save failed"))
            }
        }
    }

    fun delete() {
        val id = editingId ?: return
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _form.update { it.copy(isLoading = true) }
            try {
                val storageRef = storage.reference.child("users/$uid/items/$id.jpg")
                runCatching { storageRef.delete().await() }
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
                _events.tryEmit(UiEvent.ShowMessage(t.localizedMessage ?: "Delete failed"))
            }
        }
    }

    private fun compressImage(context: Context, uri: Uri): ByteArray {
        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

        val targetSize = 1280
        val longEdge = max(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
        var sample = 1
        while (longEdge / sample > targetSize) sample *= 2

        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap: Bitmap = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        } ?: error("Could not decode image")

        return ByteArrayOutputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
            bitmap.recycle()
            stream.toByteArray()
        }
    }
}
