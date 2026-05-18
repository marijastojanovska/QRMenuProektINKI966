package mk.qrmenu.qrmenumanager.main.orders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import mk.qrmenu.qrmenumanager.R
import mk.qrmenu.qrmenumanager.databinding.FragmentOrdersBinding
import mk.qrmenu.qrmenumanager.model.OrderStatus

class OrdersFragment : Fragment() {

    private var _binding: FragmentOrdersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OrdersViewModel by viewModels()
    private lateinit var adapter: OrderAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? AppCompatActivity)?.supportActionBar?.setTitle(R.string.title_orders)

        adapter = OrderAdapter(
            onAccept = { order -> viewModel.setStatus(order, OrderStatus.ACCEPTED) },
            onReject = { order -> viewModel.setStatus(order, OrderStatus.REJECTED) },
        )

        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter

        binding.groupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            viewModel.setStatusFilter(filterStatusFromChipId(checkedIds.firstOrNull()))
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.orders.collect { list ->
                    adapter.submitList(list)
                    binding.txtEmpty.isVisible = list.isEmpty()
                }
            }
        }
    }

    private fun filterStatusFromChipId(id: Int?): OrderStatus? = when (id) {
        R.id.chip_filter_pending -> OrderStatus.PENDING
        R.id.chip_filter_accepted -> OrderStatus.ACCEPTED
        R.id.chip_filter_rejected -> OrderStatus.REJECTED
        else -> null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recycler.adapter = null
        _binding = null
    }
}
