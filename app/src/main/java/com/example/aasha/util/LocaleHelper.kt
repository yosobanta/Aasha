package com.example.aasha.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

import android.content.ContextWrapper

object LocaleHelper {
    fun wrapContext(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)
        
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val localizedContext = context.createConfigurationContext(configuration)
            object : ContextWrapper(context) {
                override fun getResources() = localizedContext.resources
                override fun getAssets() = localizedContext.assets
                override fun getSystemService(name: String) = localizedContext.getSystemService(name)
                override fun createConfigurationContext(overrideConfiguration: Configuration) = 
                    localizedContext.createConfigurationContext(overrideConfiguration)
            }
        } else {
            val resources = context.resources
            @Suppress("DEPRECATION")
            configuration.locale = locale
            @Suppress("DEPRECATION")
            resources.updateConfiguration(configuration, resources.displayMetrics)
            context
        }
    }

    // Still keep applyLocale for quick updates if needed, but wrapContext is preferred
    fun applyLocale(context: Context, lang: String) {
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val resources = context.resources
        val config = resources.configuration
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}
