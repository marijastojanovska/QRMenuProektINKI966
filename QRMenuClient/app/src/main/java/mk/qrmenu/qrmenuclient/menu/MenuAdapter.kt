package mk.qrmenu.qrmenuclient.menu

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import mk.qrmenu.qrmenuclient.R
import mk.qrmenu.qrmenuclient.databinding.ItemMenuProductBinding
import mk.qrmenu.qrmenuclient.model.Product
import java.text.NumberFormat
import java.util.Locale

class MenuAdapter : ListAdapter<Product, MenuAdapter.ProductViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemMenuProductBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ProductViewHolder(
        private val binding: ItemMenuProductBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        private val priceFormatter: NumberFormat =
            NumberFormat.getCurrencyInstance(Locale.US)

        fun bind(product: Product) {
            binding.txtTitle.text = product.title
            binding.txtPrice.text = priceFormatter.format(product.price)
            binding.txtDescription.text = product.description

            Glide.with(binding.imgProduct)
                .load(product.imageUrl.takeIf { it.isNotBlank() })
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .centerCrop()
                .into(binding.imgProduct)
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean =
            oldItem == newItem
    }
}
