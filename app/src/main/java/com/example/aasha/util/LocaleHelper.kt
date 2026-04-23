package com.example.aasha.util

import android.content.Context
import android.os.Build
import java.util.Locale

object LocaleHelper {
    fun applyLocale(context: Context, lang: String) {
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val resources = context.resources
        val config = resources.configuration
        config.setLocale(locale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(config)
        }
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}
