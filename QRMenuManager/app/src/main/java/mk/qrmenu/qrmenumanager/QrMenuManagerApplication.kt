package mk.qrmenu.qrmenumanager

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import mk.qrmenu.qrmenumanager.notifications.OrdersNotifier

class QrMenuManagerApplication : Application() {

    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        val uid = auth.currentUser?.uid
        if (uid != null) {
            OrdersNotifier.start(this, uid)
        } else {
            OrdersNotifier.stop()
        }
    }

    override fun onCreate() {
        super.onCreate()
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener)
    }
}
