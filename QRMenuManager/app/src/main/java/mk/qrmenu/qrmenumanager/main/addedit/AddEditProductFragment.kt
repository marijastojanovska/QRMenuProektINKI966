package mk.qrmenu.qrmenumanager.main.addedit

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch
import mk.qrmenu.qrmenumanager.R
import mk.qrmenu.qrmenumanager.databinding.FragmentAddEditProductBinding

class AddEditProductFragment : Fragment() {

    private var _binding: FragmentAddEditProductBinding? = null
    private val binding get() = _binding!!

    private val args: AddEditProductFragmentArgs by navArgs()
    private val viewModel: AddEditProductViewModel by viewModels()

    private var lastLoadedImageUrl: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAddEditProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val productId = args.productId
        val isEdit = productId != null

        (activity as? AppCompatActivity)?.supportActionBar?.title =
            getString(if (isEdit) R.string.title_edit_product else R.string.title_add_product)

        binding.btnDelete.isVisible = isEdit

        viewModel.setEditMode(productId)

        binding.inputTitle.addTextChangedListener(simpleWatcher { viewModel.onTitleChanged(it) })
        binding.inputDescription.addTextChangedListener(simpleWatcher { viewModel.onDescriptionChanged(it) })
        binding.inputPrice.addTextChangedListener(simpleWatcher { viewModel.onPriceChanged(it) })
        binding.inputImageUrl.addTextChangedListener(simpleWatcher { viewModel.onImageUrlChanged(it) })

        binding.btnSave.setOnClickListener {
            viewModel.save()
        }

        binding.btnDelete.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.confirm_delete_title)
                .setMessage(R.string.confirm_delete_message)
                .setPositiveButton(R.string.action_delete) { _, _ -> viewModel.delete() }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.form.collect { state ->
                        binding.progress.isVisible = state.isLoading
                        binding.btnSave.isEnabled = !state.isLoading
                        binding.btnDelete.isEnabled = !state.isLoading

                        binding.inputTitle.setTextIfChanged(state.title)
                        binding.inputDescription.setTextIfChanged(state.description)
                        binding.inputPrice.setTextIfChanged(state.priceText)
                        binding.inputImageUrl.setTextIfChanged(state.imageUrl)

                        updatePreview(state.imageUrl)
                    }
                }

                launch {
                    viewModel.errors.collect { errors ->
                        binding.layoutTitle.error = errors.titleErr?.let { getString(it) }
                        binding.layoutPrice.error = errors.priceErr?.let { getString(it) }
                        binding.txtImageError.text = errors.imageErr?.let { getString(it) }
                        binding.txtImageError.isVisible = errors.imageErr != null
                    }
                }

                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is UiEvent.NavigateBack -> findNavController().popBackStack()
                            is UiEvent.ShowMessage ->
                                Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun updatePreview(url: String) {
        val trimmed = url.trim()
        if (trimmed == lastLoadedImageUrl) return
        lastLoadedImageUrl = trimmed
        if (trimmed.isBlank()) {
            Glide.with(this).clear(binding.imgPreview)
            binding.imgPreview.setImageResource(R.drawable.ic_image_placeholder)
        } else {
            Glide.with(this)
                .load(trimmed)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .centerCrop()
                .into(binding.imgPreview)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun EditText.setTextIfChanged(value: String) {
        if (text?.toString() != value) {
            setText(value)
            setSelection(value.length)
        }
    }

    private fun simpleWatcher(onChange: (String) -> Unit): TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            onChange(s?.toString().orEmpty())
        }
    }
}
