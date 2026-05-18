package mk.qrmenu.qrmenuclient.notifications

import android.content.Context

class OrderStatusTracker(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun get(orderId: String): String? = prefs.getString(orderId, null)

    fun put(orderId: String, status: String) {
        prefs.edit().putString(orderId, status).apply()
    }

    companion object {
        private const val PREFS_NAME = "order_status_tracker"
    }
}
