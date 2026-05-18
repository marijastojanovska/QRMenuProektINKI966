package mk.qrmenu.qrmenuclient.cart

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import mk.qrmenu.qrmenuclient.databinding.ItemCartBinding
import mk.qrmenu.qrmenuclient.model.OrderItem
import java.text.NumberFormat
import java.util.Locale

class CartAdapter(
    private val onIncrement: (String) -> Unit,
    private val onDecrement: (String) -> Unit,
    private val onRemove: (String) -> Unit,
) : ListAdapter<OrderItem, CartAdapter.CartViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = ItemCartBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CartViewHolder(binding, onIncrement, onDecrement, onRemove)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CartViewHolder(
        private val binding: ItemCartBinding,
        private val onIncrement: (String) -> Unit,
        private val onDecrement: (String) -> Unit,
        private val onRemove: (String) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        private val priceFormatter: NumberFormat =
            NumberFormat.getCurrencyInstance(Locale.US)

        private var boundProductId: String = ""

        init {
            binding.btnIncrement.setOnClickListener {
                onIncrement(boundProductId)
            }
            binding.btnDecrement.setOnClickListener {
                onDecrement(boundProductId)
            }
            binding.btnRemove.setOnClickListener {
                onRemove(boundProductId)
            }
        }

        fun bind(entry: OrderItem) {
            boundProductId = entry.productId
            binding.txtTitle.text = entry.title
            binding.txtUnitPrice.text = priceFormatter.format(entry.price)
            binding.txtLineTotal.text = priceFormatter.format(entry.price * entry.quantity)
            binding.txtQuantity.text = entry.quantity.toString()
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<OrderItem>() {
        override fun areItemsTheSame(oldItem: OrderItem, newItem: OrderItem): Boolean =
            oldItem.productId == newItem.productId

        override fun areContentsTheSame(oldItem: OrderItem, newItem: OrderItem): Boolean =
            oldItem == newItem
    }
}
