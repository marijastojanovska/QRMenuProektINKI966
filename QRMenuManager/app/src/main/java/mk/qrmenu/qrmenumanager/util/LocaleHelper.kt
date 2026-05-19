package mk.qrmenu.qrmenumanager.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {

    const val LANG_ENGLISH = "en"
    const val LANG_MACEDONIAN = "mk"

    private const val PREFS_NAME = "settings"
    private const val KEY_LANGUAGE = "language"

    fun getSavedLanguage(context: Context): String {
        val saved = context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, null)
        return saved ?: defaultLanguage()
    }

    fun setLanguage(context: Context, language: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, language)
            .apply()
    }

    fun applySavedLocale(context: Context): Context =
        applyLocale(context, getSavedLanguage(context))

    private fun applyLocale(context: Context, language: String): Context {
        val locale = Locale.forLanguageTag(language)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    private fun defaultLanguage(): String {
        val systemLang = Locale.getDefault().language
        return if (systemLang == LANG_MACEDONIAN) LANG_MACEDONIAN else LANG_ENGLISH
    }
}
