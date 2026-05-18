package mk.qrmenu.qrmenuclient.menu

import android.view.LayoutInflater
import android.view.View
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

class MenuAdapter(
    private val onAdd: (Product) -> Unit,
    private val onIncrement: (Product) -> Unit,
    private val onDecrement: (Product) -> Unit,
) : ListAdapter<Product, MenuAdapter.ProductViewHolder>(DiffCallback) {

    private var quantities: Map<String, Int> = emptyMap()

    fun setCartQuantities(newQuantities: Map<String, Int>) {
        val previous = quantities
        quantities = newQuantities
        for (i in 0 until itemCount) {
            val id = getItem(i).id
            if ((previous[id] ?: 0) != (newQuantities[id] ?: 0)) {
                notifyItemChanged(i)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemMenuProductBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ProductViewHolder(binding, onAdd, onIncrement, onDecrement)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = getItem(position)
        holder.bind(product, quantities[product.id] ?: 0)
    }

    class ProductViewHolder(
        private val binding: ItemMenuProductBinding,
        private val onAdd: (Product) -> Unit,
        private val onIncrement: (Product) -> Unit,
        private val onDecrement: (Product) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        private val priceFormatter: NumberFormat =
            NumberFormat.getCurrencyInstance(Locale.US)

        private var boundProduct: Product? = null

        init {
            binding.btnAdd.setOnClickListener {
                boundProduct?.let(onAdd)
            }
            binding.btnIncrement.setOnClickListener {
                boundProduct?.let(onIncrement)
            }
            binding.btnDecrement.setOnClickListener {
                boundProduct?.let(onDecrement)
            }
        }

        fun bind(product: Product, quantity: Int) {
            boundProduct = product
            binding.txtTitle.text = product.title
            binding.txtPrice.text = priceFormatter.format(product.price)
            binding.txtDescription.text = product.description

            Glide.with(binding.imgProduct)
                .load(product.imageUrl.takeIf { it.isNotBlank() })
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .centerCrop()
                .into(binding.imgProduct)

            if (quantity > 0) {
                binding.btnAdd.visibility = View.GONE
                binding.quantityContainer.visibility = View.VISIBLE
                binding.txtQuantity.text = quantity.toString()
            } else {
                binding.btnAdd.visibility = View.VISIBLE
                binding.quantityContainer.visibility = View.GONE
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean =
            oldItem == newItem
    }
}
