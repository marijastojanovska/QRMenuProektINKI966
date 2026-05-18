package mk.qrmenu.qrmenumanager.notifications

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import mk.qrmenu.qrmenumanager.R
import mk.qrmenu.qrmenumanager.main.MainActivity
import mk.qrmenu.qrmenumanager.model.Order
import mk.qrmenu.qrmenumanager.model.OrderStatus

object OrdersNotifier {

    private const val CHANNEL_ID = "orders_new"

    private var registration: ListenerRegistration? = null
    private var isFirstSnapshot = true
    private val seenIds = mutableSetOf<String>()

    fun start(context: Context, managerId: String) {
        if (registration != null) return
        ensureChannel(context)
        isFirstSnapshot = true
        seenIds.clear()

        registration = FirebaseFirestore.getInstance()
            .collection("orders")
            .document("manager")
            .collection(managerId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                if (isFirstSnapshot) {
                    snapshot.documents.forEach { seenIds.add(it.id) }
                    isFirstSnapshot = false
                    return@addSnapshotListener
                }

                for (change in snapshot.documentChanges) {
                    if (change.type != DocumentChange.Type.ADDED) continue
                    val id = change.document.id
                    if (id in seenIds) continue
                    seenIds.add(id)

                    val order = change.document.toObject(Order::class.java)
                    if (order.status == OrderStatus.PENDING.name) {
                        postNotification(context, order)
                    }
                }
            }
    }

    fun stop() {
        registration?.remove()
        registration = null
        isFirstSnapshot = true
        seenIds.clear()
    }

    private fun ensureChannel(context: Context) {
        val channel = NotificationChannelCompat.Builder(
            CHANNEL_ID,
            NotificationManager.IMPORTANCE_HIGH,
        )
            .setName(context.getString(R.string.channel_new_orders_name))
            .setDescription(context.getString(R.string.channel_new_orders_description))
            .build()
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    private fun postNotification(context: Context, order: Order) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_ORDERS, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            order.id.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val itemsSummary = order.items.joinToString(", ") { "${it.quantity}× ${it.title}" }
        val contentText = itemsSummary.ifBlank {
            context.getString(R.string.notification_new_order_fallback)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_new_order_title))
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(context).notify(order.id.hashCode(), notification)
    }

    const val EXTRA_OPEN_ORDERS = "extra_open_orders"
}
