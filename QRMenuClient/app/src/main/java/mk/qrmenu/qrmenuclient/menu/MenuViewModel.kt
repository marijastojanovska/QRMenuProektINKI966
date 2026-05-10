package mk.qrmenu.qrmenuclient.menu

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mk.qrmenu.qrmenuclient.data.CachedMenuRepository
import mk.qrmenu.qrmenuclient.data.MenuRepository
import mk.qrmenu.qrmenuclient.data.local.AppDatabase
import mk.qrmenu.qrmenuclient.data.local.CachedMenuSummary
import mk.qrmenu.qrmenuclient.model.Category
import mk.qrmenu.qrmenuclient.model.Product

class MenuViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val remoteRepository = MenuRepository()
    private val cachedRepository = CachedMenuRepository(
        AppDatabase.getInstance(application).cachedMenuDao()
    )

    private val _uiState = MutableStateFlow<MenuUiState>(MenuUiState.Idle)
    val uiState: StateFlow<MenuUiState> = _uiState.asStateFlow()

    val cachedMenus: StateFlow<List<CachedMenuSummary>> = cachedRepository.observeSummaries()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var lastUserId: String? = null
    private var allItems: List<Product> = emptyList()
    private var selectedCategory: Category? = null

    fun loadMenu(userId: String) {
        val trimmed = userId.trim()

        if (trimmed.isEmpty()) {
            _uiState.value = MenuUiState.Error("Empty QR code")
            return
        }

        lastUserId = trimmed
        selectedCategory = null
        allItems = emptyList()

        _uiState.value = MenuUiState.Loading

        viewModelScope.launch {
            try {
                val items = remoteRepository.getMenu(trimmed)
                allItems = items
                cachedRepository.saveMenu(trimmed, items, System.currentTimeMillis())
                emitSuccess()
            } catch (t: Throwable) {
                _uiState.value = MenuUiState.Error(t.localizedMessage ?: "Unknown error")
            }
        }
    }

    fun loadCachedMenu(userId: String) {
        val trimmed = userId.trim()

        if (trimmed.isEmpty()) return

        lastUserId = trimmed
        selectedCategory = null
        allItems = emptyList()

        _uiState.value = MenuUiState.Loading

        viewModelScope.launch {
            try {
                allItems = cachedRepository.getMenu(trimmed)
                emitSuccess()
            } catch (t: Throwable) {
                _uiState.value = MenuUiState.Error(t.localizedMessage ?: "Unknown error")
            }
        }
    }

    fun selectCategory(category: Category?) {
        if (selectedCategory == category) return

        selectedCategory = category

        if (_uiState.value is MenuUiState.Success) {
            emitSuccess()
        }
    }

    fun retry() {
        lastUserId?.let(::loadMenu)
    }

    private fun emitSuccess() {
        val available = allItems.mapNotNullTo(sortedSetOf(compareBy { it.sortOrder })) {
            it.categoryEnum
        }

        val filtered = allItems.filter { product ->
            val pickedCategory = selectedCategory
            pickedCategory == null || product.categoryEnum == pickedCategory
        }

        val sorted = filtered.sortedWith(
            compareBy(
                { it.categoryEnum?.sortOrder ?: Int.MAX_VALUE },
                { it.title.lowercase() },
            ),
        )

        _uiState.value = MenuUiState.Success(
            items = sorted,
            availableCategories = available,
            selectedCategory = selectedCategory,
        )
    }
}
