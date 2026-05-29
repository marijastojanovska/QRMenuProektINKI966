package mk.qrmenu.qrmenuclient.cart

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import mk.qrmenu.qrmenuclient.R
import mk.qrmenu.qrmenuclient.databinding.FragmentCartBinding
import java.text.NumberFormat
import java.util.Locale

class CartFragment : Fragment() {

    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CartViewModel by viewModels()

    private val adapter by lazy {
        CartAdapter(
            onIncrement = viewModel::increment,
            onDecrement = viewModel::decrement,
            onRemove = viewModel::remove,
        )
    }

    private val priceFormatter: NumberFormat =
        NumberFormat.getCurrencyInstance(Locale.US)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerCart.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerCart.adapter = adapter

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnPlaceOrder.setOnClickListener {
            findNavController().navigate(R.id.checkoutFragment)
        }

        observeCart()
    }

    private fun observeCart() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.cartState.collect(::renderCart)
            }
        }
    }

    private fun renderCart(state: CartState) {
        adapter.submitList(state.entries)
        binding.txtTotal.text = priceFormatter.format(state.totalPrice)

        if (state.isEmpty) {
            binding.recyclerCart.visibility = View.GONE
            binding.txtEmpty.visibility = View.VISIBLE
            binding.btnPlaceOrder.isEnabled = false
            binding.checkoutContainer.visibility = View.GONE
            binding.divider.visibility = View.GONE
        } else {
            binding.recyclerCart.visibility = View.VISIBLE
            binding.txtEmpty.visibility = View.GONE
            binding.checkoutContainer.visibility = View.VISIBLE
            binding.divider.visibility = View.VISIBLE
            binding.btnPlaceOrder.isEnabled = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerCart.adapter = null
        _binding = null
    }
}
