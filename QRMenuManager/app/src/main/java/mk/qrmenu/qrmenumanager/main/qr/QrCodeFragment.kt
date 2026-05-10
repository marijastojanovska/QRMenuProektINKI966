package mk.qrmenu.qrmenumanager.main.qr

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import mk.qrmenu.qrmenumanager.R
import mk.qrmenu.qrmenumanager.databinding.FragmentQrCodeBinding

class QrCodeFragment : Fragment() {

    private var _binding: FragmentQrCodeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: QrCodeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentQrCodeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? AppCompatActivity)?.supportActionBar?.setTitle(R.string.title_qr_code)

//        binding.btnShare.setOnClickListener {
//            val content = viewModel.state.value.content
//            if (content.isNotBlank()) {
//                val intent = Intent(Intent.ACTION_SEND).apply {
//                    type = "text/plain"
//                    putExtra(Intent.EXTRA_TEXT, content)
//                }
//                startActivity(Intent.createChooser(intent, getString(R.string.action_share_id)))
//            }
//        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    binding.progress.isVisible = state.isLoading
                    binding.imgQr.isVisible = state.bitmap != null
                    //binding.txtUrl.isVisible = state.content.isNotBlank()
                    //binding.btnShare.isEnabled = state.content.isNotBlank()

                    state.bitmap?.let { binding.imgQr.setImageBitmap(it) }
                    //binding.txtUrl.text = state.content
                    state.errorMessage?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.imgQr.setImageDrawable(null)
        _binding = null
    }
}
