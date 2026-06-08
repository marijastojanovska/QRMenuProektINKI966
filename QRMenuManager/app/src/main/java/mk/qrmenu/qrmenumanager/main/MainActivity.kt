package mk.qrmenu.qrmenumanager.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.facebook.login.LoginManager
import com.google.firebase.auth.FirebaseAuth
import mk.qrmenu.qrmenumanager.R
import mk.qrmenu.qrmenumanager.auth.AuthActivity
import mk.qrmenu.qrmenumanager.databinding.ActivityMainBinding
import mk.qrmenu.qrmenumanager.notifications.OrdersNotifier
import mk.qrmenu.qrmenumanager.util.LocaleHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navController: NavController

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applySavedLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.appBar) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNav) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(bottom = systemBars.bottom)
            insets
        }

        setSupportActionBar(binding.toolbar)

        val navHost = supportFragmentManager.findFragmentById(R.id.main_nav_host) as NavHostFragment
        navController = navHost.navController
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.menuFragment, R.id.ordersFragment)
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.bottomNav.setupWithNavController(navController)

        maybeRequestNotificationPermission()
        handleNotificationIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(OrdersNotifier.EXTRA_OPEN_ORDERS, false) == true) {

            if (navController.currentDestination?.id != R.id.ordersFragment) {
                navController.navigate(R.id.ordersFragment)
            }

            intent.removeExtra(OrdersNotifier.EXTRA_OPEN_ORDERS)
        }
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_generate_qr -> {
                if (navController.currentDestination?.id != R.id.qrCodeFragment) {
                    navController.navigate(R.id.qrCodeFragment)
                }
                true
            }
            R.id.action_language -> {
                showLanguageDialog()
                true
            }
            R.id.action_logout -> {
                OrdersNotifier.stop()
                LoginManager.getInstance().logOut()
                FirebaseAuth.getInstance().signOut()
                startActivity(
                    Intent(this, AuthActivity::class.java).addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    )
                )
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showLanguageDialog() {
        val labels = arrayOf(
            getString(R.string.language_english),
            getString(R.string.language_macedonian),
        )
        val codes = arrayOf(LocaleHelper.LANG_ENGLISH, LocaleHelper.LANG_MACEDONIAN)
        val currentIndex = codes.indexOf(LocaleHelper.getSavedLanguage(this)).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle(R.string.action_language)
            .setSingleChoiceItems(labels, currentIndex) { dialog, which ->
                dialog.dismiss()
                val selected = codes[which]
                if (selected != LocaleHelper.getSavedLanguage(this)) {
                    LocaleHelper.setLanguage(this, selected)
                    recreate()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
