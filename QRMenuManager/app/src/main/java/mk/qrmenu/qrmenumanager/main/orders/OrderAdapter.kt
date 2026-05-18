package mk.qrmenu.qrmenumanager.main.orders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import mk.qrmenu.qrmenumanager.R
import mk.qrmenu.qrmenumanager.databinding.ItemOrderBinding
import mk.qrmenu.qrmenumanager.model.Order
import mk.qrmenu.qrmenumanager.model.OrderItem
import mk.qrmenu.qrmenumanager.model.OrderStatus
import java.text.DateFormat
import java.util.Locale

class OrderAdapter(
    private val onAccept: (Order) -> Unit,
    private val onReject: (Order) -> Unit,
) : ListAdapter<Order, OrderAdapter.OrderViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return OrderViewHolder(binding, onAccept, onReject)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class OrderViewHolder(
        private val binding: ItemOrderBinding,
        private val onAccept: (Order) -> Unit,
        private val onReject: (Order) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormatter: DateFormat =
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)

        fun bind(order: Order) {
            val context = binding.root.context
            val status = OrderStatus.fromStorage(order.status)

            binding.txtCreatedAt.text = order.createdAt?.let { dateFormatter.format(it) } ?: ""

            binding.txtStatus.text = when (status) {
                OrderStatus.PENDING -> context.getString(R.string.status_pending)
                OrderStatus.ACCEPTED -> context.getString(R.string.status_accepted)
                OrderStatus.REJECTED -> context.getString(R.string.status_rejected)
                null -> order.status
            }

            binding.txtItems.text = formatItems(order.items)

            val total = order.items.sumOf { it.price * it.quantity }
            binding.txtTotal.text = context.getString(R.string.order_total_format, total)

            val isPending = status == OrderStatus.PENDING
            binding.actionRow.visibility = if (isPending) View.VISIBLE else View.GONE

            binding.btnAccept.setOnClickListener { onAccept(order) }
            binding.btnReject.setOnClickListener { onReject(order) }
        }

        private fun formatItems(items: List<OrderItem>): String {
            if (items.isEmpty()) return ""
            return items.joinToString(separator = " · ") {
                String.format(
                    Locale.getDefault(),
                    "%d× %s",
                    it.quantity,
                    it.title,
                )
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Order>() {
            override fun areItemsTheSame(oldItem: Order, newItem: Order): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Order, newItem: Order): Boolean =
                oldItem == newItem
        }
    }
}
