package mk.qrmenu.qrmenumanager.main.menu

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import mk.qrmenu.qrmenumanager.R
import mk.qrmenu.qrmenumanager.databinding.ItemProductBinding
import mk.qrmenu.qrmenumanager.model.Product
import java.util.Locale

class ProductAdapter(
    private val onClick: (Product) -> Unit,
) : ListAdapter<Product, ProductAdapter.ProductViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ProductViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ProductViewHolder(
        private val binding: ItemProductBinding,
        private val onClick: (Product) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product) {
            binding.txtTitle.text = product.title
            binding.txtPrice.text = String.format(Locale.getDefault(), "$%.2f", product.price)
            binding.txtDescription.text = product.description

            Glide.with(binding.imgProduct)
                .load(product.imageUrl.ifBlank { null })
                .placeholder(R.drawable.ic_image_placeholder)
                .centerCrop()
                .into(binding.imgProduct)

            binding.root.setOnClickListener { onClick(product) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Product>() {
            override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean =
                oldItem == newItem
        }
    }
}
