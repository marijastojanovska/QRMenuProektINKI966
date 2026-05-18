package mk.qrmenu.qrmenuclient

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.content.getSystemService
import mk.qrmenu.qrmenuclient.notifications.OrderNotificationManager

class QrMenuApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createOrderUpdatesChannel()
        OrderNotificationManager.start(this)
    }

    private fun createOrderUpdatesChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = getSystemService<NotificationManager>() ?: return
        if (manager.getNotificationChannel(OrderNotificationManager.CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            OrderNotificationManager.CHANNEL_ID,
            getString(R.string.notification_channel_order_updates),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = getString(R.string.notification_channel_order_updates_description)
        }
        manager.createNotificationChannel(channel)
    }
}
