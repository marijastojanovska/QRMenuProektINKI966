package mk.qrmenu.qrmenuclient.orders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import mk.qrmenu.qrmenuclient.R
import mk.qrmenu.qrmenuclient.databinding.ItemOrderBinding
import mk.qrmenu.qrmenuclient.model.Order
import mk.qrmenu.qrmenuclient.model.OrderStatus
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Locale

class OrderAdapter : ListAdapter<Order, OrderAdapter.OrderViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class OrderViewHolder(
        private val binding: ItemOrderBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        private val priceFormatter: NumberFormat =
            NumberFormat.getCurrencyInstance(Locale.US)

        private val dateFormatter: DateFormat =
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)

        fun bind(order: Order) {
            val context = binding.root.context

            binding.txtCreatedAt.text = order.createdAt
                ?.let(dateFormatter::format)
                ?: context.getString(R.string.order_pending_timestamp)

            val itemsLine = order.items.joinToString(" · ") {
                context.getString(R.string.order_items_format, it.quantity, it.title)
            }
            binding.txtItems.text = itemsLine.ifBlank { "—" }

            val total = order.items.sumOf { it.price * it.quantity }
            binding.txtTotal.text = context.getString(
                R.string.order_total_format,
                priceFormatter.format(total),
            )

            val statusEnum = OrderStatus.fromStorage(order.status)
            val (label, colorRes) = when (statusEnum) {
                OrderStatus.PENDING -> R.string.status_pending to R.color.status_pending
                OrderStatus.ACCEPTED -> R.string.status_accepted to R.color.status_accepted
                OrderStatus.REJECTED -> R.string.status_rejected to R.color.status_rejected
                null -> R.string.status_pending to R.color.status_pending
            }
            binding.txtStatus.setText(label)
            binding.txtStatus.setTextColor(ContextCompat.getColor(context, colorRes))
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Order>() {
        override fun areItemsTheSame(oldItem: Order, newItem: Order): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Order, newItem: Order): Boolean =
            oldItem == newItem
    }
}
