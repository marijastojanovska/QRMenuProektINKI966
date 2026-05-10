package mk.qrmenu.qrmenuclient.menu.history

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import mk.qrmenu.qrmenuclient.R
import mk.qrmenu.qrmenuclient.data.local.CachedMenuSummary
import mk.qrmenu.qrmenuclient.databinding.ItemCachedMenuBinding

class CachedMenusAdapter(
    private val onClick: (CachedMenuSummary) -> Unit,
) : ListAdapter<CachedMenuSummary, CachedMenusAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCachedMenuBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemCachedMenuBinding,
        private val onClick: (CachedMenuSummary) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(summary: CachedMenuSummary) {
            binding.txtCachedId.text = formatUserId(summary.userId)
            val relative = DateUtils.getRelativeTimeSpanString(
                summary.lastScannedAt,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE,
            )
            binding.txtCachedSubtitle.text = binding.root.context.getString(
                R.string.cached_menu_subtitle,
                summary.itemCount,
                relative,
            )
            binding.root.setOnClickListener { onClick(summary) }
        }

        private fun formatUserId(userId: String): String =
            if (userId.length <= 20) userId
            else userId.take(8) + "…" + userId.takeLast(8)
    }

    private object DiffCallback : DiffUtil.ItemCallback<CachedMenuSummary>() {
        override fun areItemsTheSame(oldItem: CachedMenuSummary, newItem: CachedMenuSummary): Boolean =
            oldItem.userId == newItem.userId

        override fun areContentsTheSame(oldItem: CachedMenuSummary, newItem: CachedMenuSummary): Boolean =
            oldItem == newItem
    }
}
