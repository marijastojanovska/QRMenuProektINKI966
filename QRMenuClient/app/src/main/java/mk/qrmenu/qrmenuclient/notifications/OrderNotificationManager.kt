package mk.qrmenu.qrmenuclient.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import mk.qrmenu.qrmenuclient.MainActivity
import mk.qrmenu.qrmenuclient.R
import mk.qrmenu.qrmenuclient.data.ClientIdProvider
import mk.qrmenu.qrmenuclient.data.OrderRepository
import mk.qrmenu.qrmenuclient.model.Order
import mk.qrmenu.qrmenuclient.model.OrderStatus

object OrderNotificationManager {

    const val CHANNEL_ID = "order_updates"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    fun start(context: Context) {
        if (job != null) return

        val appContext = context.applicationContext
        val clientId = ClientIdProvider.get(appContext)
        val tracker = OrderStatusTracker(appContext)
        val repository = OrderRepository()

        job = scope.launch {
            repository.observeClientOrders(clientId).collect { orders ->
                processOrders(appContext, tracker, orders)
            }
        }
    }

    private fun processOrders(
        context: Context,
        tracker: OrderStatusTracker,
        orders: List<Order>,
    ) {
        for (order in orders) {
            if (order.id.isBlank()) continue

            val previous = tracker.get(order.id)
            val current = order.status

            if (previous == OrderStatus.PENDING.name &&
                (current == OrderStatus.ACCEPTED.name || current == OrderStatus.REJECTED.name)
            ) {
                postNotification(context, order)
            }

            tracker.put(order.id, current)
        }
    }

    private fun postNotification(context: Context, order: Order) {
        val notificationManager = NotificationManagerCompat.from(context)
        if (!notificationManager.areNotificationsEnabled()) return

        val accepted = order.status == OrderStatus.ACCEPTED.name
        val titleRes = if (accepted) {
            R.string.notification_order_accepted_title
        } else {
            R.string.notification_order_rejected_title
        }
        val bodyRes = if (accepted) {
            R.string.notification_order_accepted_body
        } else {
            R.string.notification_order_rejected_body
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_NAVIGATE_TO, MainActivity.NAV_TARGET_ORDERS)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            order.id.hashCode(),
            tapIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_receipt)
            .setContentTitle(context.getString(titleRes))
            .setContentText(context.getString(bodyRes))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            notificationManager.notify(order.id.hashCode(), notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS was revoked between the check and the call. Ignore.
        }
    }
}
