package mk.qrmenu.qrmenuclient.checkout

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import mk.qrmenu.qrmenuclient.R
import mk.qrmenu.qrmenuclient.cart.CartState
import mk.qrmenu.qrmenuclient.databinding.FragmentCheckoutBinding
import mk.qrmenu.qrmenuclient.model.PaymentMethod
import java.text.NumberFormat
import java.util.Locale

class CheckoutFragment : Fragment() {

    private var _binding: FragmentCheckoutBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CheckoutViewModel by viewModels()

    private val priceFormatter: NumberFormat =
        NumberFormat.getCurrencyInstance(Locale.US)

    private var processingDialog: Dialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCheckoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.radioPayment.setOnCheckedChangeListener { _, checkedId ->
            binding.cardDetailsContainer.visibility =
                if (checkedId == R.id.radio_card) View.VISIBLE else View.GONE
        }

        binding.editCardExpiry.addTextChangedListener(ExpiryDateFormatter(binding.editCardExpiry))

        binding.btnSubmit.setOnClickListener { onSubmitClicked() }

        observeCart()
        observeState()
    }

    private fun observeCart() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.cartState.collect(::renderCart)
            }
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::renderState)
            }
        }
    }

    private fun renderCart(state: CartState) {
        binding.txtTotal.text = priceFormatter.format(state.totalPrice)
    }

    private fun renderState(state: CheckoutUiState) {
        when (state) {
            CheckoutUiState.Idle -> {
                dismissProcessingDialog()
                binding.btnSubmit.isEnabled = true
            }
            CheckoutUiState.ProcessingPayment -> {
                binding.btnSubmit.isEnabled = false
                showProcessingDialog()
            }
            CheckoutUiState.Submitting -> {
                binding.btnSubmit.isEnabled = false
            }
            is CheckoutUiState.Success -> {
                dismissProcessingDialog()
                viewModel.consumeState()
                Snackbar.make(
                    binding.root,
                    R.string.order_placed_success,
                    Snackbar.LENGTH_LONG,
                ).show()
                findNavController().navigate(
                    R.id.ordersFragment,
                    null,
                    androidx.navigation.navOptions {
                        popUpTo(R.id.menuFragment) { inclusive = false }
                    },
                )
            }
            is CheckoutUiState.Error -> {
                dismissProcessingDialog()
                binding.btnSubmit.isEnabled = true
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG)
                    .setAction(R.string.action_retry) { onSubmitClicked() }
                    .show()
                viewModel.consumeState()
            }
        }
    }

    private fun onSubmitClicked() {
        val address = binding.editAddress.text?.toString().orEmpty().trim()
        val city = binding.editCity.text?.toString().orEmpty().trim()
        val phone = binding.editPhone.text?.toString().orEmpty().trim()

        val requiredError = getString(R.string.error_required_field)
        binding.tilAddress.error = if (address.isEmpty()) requiredError else null
        binding.tilCity.error = if (city.isEmpty()) requiredError else null
        binding.tilPhone.error = if (phone.isEmpty()) requiredError else null

        var hasError = address.isEmpty() || city.isEmpty() || phone.isEmpty()

        val paymentMethod = if (binding.radioCard.isChecked) PaymentMethod.CARD else PaymentMethod.CASH

        if (paymentMethod == PaymentMethod.CARD) {
            val number = binding.editCardNumber.text?.toString().orEmpty().trim()
            val expiry = binding.editCardExpiry.text?.toString().orEmpty().trim()
            val cvv = binding.editCardCvv.text?.toString().orEmpty().trim()
            val holder = binding.editCardHolder.text?.toString().orEmpty().trim()

            val numberValid = number.length in 13..19 && number.all(Char::isDigit)
            val expiryValid = EXPIRY_REGEX.matches(expiry)
            val cvvValid = cvv.length == 3 && cvv.all(Char::isDigit)
            val holderValid = holder.isNotEmpty()

            val invalidCard = getString(R.string.error_invalid_card)
            binding.tilCardNumber.error = if (numberValid) null else invalidCard
            binding.tilCardExpiry.error = if (expiryValid) null else invalidCard
            binding.tilCardCvv.error = if (cvvValid) null else invalidCard
            binding.tilCardHolder.error = if (holderValid) null else requiredError

            if (!numberValid || !expiryValid || !cvvValid || !holderValid) {
                hasError = true
            }
        } else {
            binding.tilCardNumber.error = null
            binding.tilCardExpiry.error = null
            binding.tilCardCvv.error = null
            binding.tilCardHolder.error = null
        }

        if (hasError) return

        viewModel.submit(address, city, phone, paymentMethod)
    }

    private fun showProcessingDialog() {
        if (processingDialog?.isShowing == true) return
        processingDialog = MaterialAlertDialogBuilder(requireContext())
            .setView(R.layout.dialog_processing_payment)
            .setCancelable(false)
            .show()
    }

    private fun dismissProcessingDialog() {
        processingDialog?.dismiss()
        processingDialog = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dismissProcessingDialog()
        _binding = null
    }

    private class ExpiryDateFormatter(
        private val editText: com.google.android.material.textfield.TextInputEditText,
    ) : TextWatcher {
        private var editing = false

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
            if (editing || s == null) return
            editing = true
            val digits = s.toString().filter(Char::isDigit).take(4)
            val formatted = when {
                digits.length >= 3 -> digits.substring(0, 2) + "/" + digits.substring(2)
                else -> digits
            }
            if (formatted != s.toString()) {
                s.replace(0, s.length, formatted)
                editText.setSelection(formatted.length)
            }
            editing = false
        }
    }

    private companion object {
        val EXPIRY_REGEX = Regex("^(0[1-9]|1[0-2])/\\d{2}$")
    }
}
