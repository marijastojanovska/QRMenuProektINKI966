package mk.qrmenu.qrmenuclient.menu.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import mk.qrmenu.qrmenuclient.databinding.FragmentCachedMenusBinding
import mk.qrmenu.qrmenuclient.menu.MenuViewModel

class CachedMenusBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentCachedMenusBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MenuViewModel by viewModels(
        ownerProducer = { requireParentFragment() },
    )

    private val adapter = CachedMenusAdapter { summary ->
        viewModel.loadCachedMenu(summary.userId)
        dismiss()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCachedMenusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerCached.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerCached.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.cachedMenus.collect { summaries ->
                    adapter.submitList(summaries)
                    binding.recyclerCached.visibility =
                        if (summaries.isEmpty()) View.GONE else View.VISIBLE
                    binding.txtCachedEmpty.visibility =
                        if (summaries.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerCached.adapter = null
        _binding = null
    }

    companion object {
        const val TAG = "CachedMenusBottomSheet"
    }
}
