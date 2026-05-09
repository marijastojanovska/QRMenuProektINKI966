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
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import mk.qrmenu.qrmenuclient.R
import mk.qrmenu.qrmenuclient.databinding.FragmentMenuBinding

class MenuFragment : Fragment() {

    private var _binding: FragmentMenuBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MenuViewModel by viewModels()
    private val adapter = MenuAdapter()

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

        binding.recyclerMenu.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerMenu.adapter = adapter

        binding.btnScan.setOnClickListener { startScan() }

        ensureScannerModuleInstalled()
        observeUiState()
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
            }
            MenuUiState.Loading -> {
                binding.progress.visibility = View.VISIBLE
                binding.recyclerMenu.visibility = View.GONE
                binding.txtEmpty.visibility = View.GONE
            }
            is MenuUiState.Success -> {
                binding.progress.visibility = View.GONE
                adapter.submitList(state.items)
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
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG)
                    .setAction(R.string.action_retry) { viewModel.retry() }
                    .show()
            }
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
