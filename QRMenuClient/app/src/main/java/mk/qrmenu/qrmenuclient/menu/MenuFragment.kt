package mk.qrmenu.qrmenuclient.menu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.launch
import mk.qrmenu.qrmenuclient.R
import mk.qrmenu.qrmenuclient.databinding.FragmentMenuBinding
import mk.qrmenu.qrmenuclient.menu.history.CachedMenusBottomSheet
import mk.qrmenu.qrmenuclient.model.Category

class MenuFragment : Fragment() {

    private var _binding: FragmentMenuBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MenuViewModel by viewModels()
    private val adapter = MenuAdapter()

    private var renderedCategories: Set<Category> = emptySet()
    private var suppressChipEvents = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerMenu.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerMenu.adapter = adapter

        binding.btnScan.setOnClickListener { startScan() }

        binding.toolbar.inflateMenu(R.menu.menu_main)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_history -> {
                    showCachedMenus()
                    true
                }
                else -> false
            }
        }

        ensureScannerModuleInstalled()
        observeUiState()
    }

    private fun showCachedMenus() {
        if (childFragmentManager.findFragmentByTag(CachedMenusBottomSheet.TAG) == null) {
            CachedMenusBottomSheet().show(childFragmentManager, CachedMenusBottomSheet.TAG)
        }
    }

    private fun startScan() {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        val scanner = GmsBarcodeScanning.getClient(requireContext(), options)
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                val rawValue = barcode.rawValue
                if (rawValue.isNullOrBlank()) {
                    showSnackbar(getString(R.string.error_invalid_qr))
                } else {
                    viewModel.loadMenu(rawValue)
                }
            }
            .addOnCanceledListener {
                // user dismissed the scanner — keep current state
            }
            .addOnFailureListener {
                showSnackbar(it.localizedMessage ?: getString(R.string.error_invalid_qr))
            }
    }

    private fun ensureScannerModuleInstalled() {
        val scanner = GmsBarcodeScanning.getClient(requireContext())
        val request = ModuleInstallRequest.newBuilder()
            .addApi(scanner)
            .build()
        ModuleInstall.getClient(requireContext()).installModules(request)
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: MenuUiState) {
        when (state) {
            MenuUiState.Idle -> {
                binding.progress.visibility = View.GONE
                binding.recyclerMenu.visibility = View.GONE
                binding.txtEmpty.visibility = View.VISIBLE
                binding.txtEmpty.setText(R.string.empty_menu_hint)
                hideFilters()
            }
            MenuUiState.Loading -> {
                binding.progress.visibility = View.VISIBLE
                binding.recyclerMenu.visibility = View.GONE
                binding.txtEmpty.visibility = View.GONE
                hideFilters()
            }
            is MenuUiState.Success -> {
                binding.progress.visibility = View.GONE
                adapter.submitList(state.items)
                renderFilters(state.availableCategories, state.selectedCategory)
                if (state.items.isEmpty()) {
                    binding.recyclerMenu.visibility = View.GONE
                    binding.txtEmpty.visibility = View.VISIBLE
                    binding.txtEmpty.setText(R.string.empty_menu_after_scan)
                } else {
                    binding.recyclerMenu.visibility = View.VISIBLE
                    binding.txtEmpty.visibility = View.GONE
                }
            }
            is MenuUiState.Error -> {
                binding.progress.visibility = View.GONE
                binding.recyclerMenu.visibility = View.GONE
                binding.txtEmpty.visibility = View.VISIBLE
                binding.txtEmpty.setText(R.string.error_load_menu)
                hideFilters()
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG)
                    .setAction(R.string.action_retry) { viewModel.retry() }
                    .show()
            }
        }
    }

    private fun renderFilters(available: Set<Category>, selected: Category?) {
        if (available.isEmpty()) {
            hideFilters()
            return
        }
        binding.scrollFilters.visibility = View.VISIBLE
        if (renderedCategories != available) {
            buildChips(available)
            renderedCategories = available
        }
        selectChip(selected)
    }

    private fun hideFilters() {
        binding.scrollFilters.visibility = View.GONE
        binding.chipGroupCategories.removeAllViews()
        renderedCategories = emptySet()
    }

    private fun buildChips(available: Set<Category>) {
        binding.chipGroupCategories.removeAllViews()
        binding.chipGroupCategories.addView(
            createFilterChip(getString(R.string.filter_all), category = null)
        )
        Category.values()
            .filter { it in available }
            .sortedBy { it.sortOrder }
            .forEach { category ->
                binding.chipGroupCategories.addView(
                    createFilterChip(category.displayName, category)
                )
            }
    }

    private fun createFilterChip(label: String, category: Category?): Chip =
        Chip(requireContext()).apply {
            text = label
            isCheckable = true
            tag = category
            setOnCheckedChangeListener { _, isChecked ->
                if (!suppressChipEvents && isChecked) {
                    viewModel.selectCategory(category)
                }
            }
        }

    private fun selectChip(target: Category?) {
        suppressChipEvents = true
        try {
            for (i in 0 until binding.chipGroupCategories.childCount) {
                val chip = binding.chipGroupCategories.getChildAt(i) as Chip
                chip.isChecked = (chip.tag as? Category) == target
            }
        } finally {
            suppressChipEvents = false
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerMenu.adapter = null
        _binding = null
    }
}
