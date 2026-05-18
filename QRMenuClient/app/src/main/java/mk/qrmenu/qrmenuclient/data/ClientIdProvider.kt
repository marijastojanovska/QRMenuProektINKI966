package mk.qrmenu.qrmenuclient.data

import android.content.Context
import java.util.UUID

object ClientIdProvider {

    private const val PREFS_NAME = "qr_menu_client_prefs"
    private const val KEY_CLIENT_ID = "client_id"

    fun get(context: Context): String {
        val prefs = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        prefs.getString(KEY_CLIENT_ID, null)?.let { return it }

        val newId = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_CLIENT_ID, newId).apply()
        return newId
    }
}
