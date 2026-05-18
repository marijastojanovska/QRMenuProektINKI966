package mk.qrmenu.qrmenuclient.orders

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
import kotlinx.coroutines.launch
import mk.qrmenu.qrmenuclient.R
import mk.qrmenu.qrmenuclient.databinding.FragmentOrdersBinding
import mk.qrmenu.qrmenuclient.model.Order
import mk.qrmenu.qrmenuclient.model.OrderStatus

class OrdersFragment : Fragment() {

    private var _binding: FragmentOrdersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OrdersViewModel by viewModels()
    private val adapter = OrderAdapter()

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

        binding.recyclerOrders.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerOrders.adapter = adapter

        binding.chipGroupFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            val status = when (checkedIds.firstOrNull()) {
                R.id.chip_filter_pending -> OrderStatus.PENDING
                R.id.chip_filter_accepted -> OrderStatus.ACCEPTED
                R.id.chip_filter_rejected -> OrderStatus.REJECTED
                else -> null
            }
            viewModel.setStatusFilter(status)
        }

        observeOrders()
    }

    private fun observeOrders() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.orders.collect(::render)
            }
        }
    }

    private fun render(orders: List<Order>) {
        adapter.submitList(orders)
        if (orders.isEmpty()) {
            binding.recyclerOrders.visibility = View.GONE
            binding.txtEmpty.visibility = View.VISIBLE
        } else {
            binding.recyclerOrders.visibility = View.VISIBLE
            binding.txtEmpty.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerOrders.adapter = null
        _binding = null
    }
}
