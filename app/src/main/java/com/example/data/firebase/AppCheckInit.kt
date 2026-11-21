package com.example.data.firebase

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory

private const val TAG = "AppCheckInit"

private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

// Dedicated metadata store to distinguish a harness-injected registered debug token
// (e.g., AI Studio emulator / CI) from an unregistered random UUID generated on a real phone.
private const val META_PREFS = "shasthosathi_appcheck_meta"
private const val KEY_HARNESS_TOKEN = "harness_debug_token"

fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }

        val firebaseApp = FirebaseApp.getInstance()
        val persistenceKey = firebaseApp.persistenceKey
        val debugStorePrefs = context.getSharedPreferences(
            "$DEBUG_STORE_PREFIX$persistenceKey",
            Context.MODE_PRIVATE
        )
        val metaPrefs = context.getSharedPreferences(META_PREFS, Context.MODE_PRIVATE)

        if (BuildConfig.DEBUG) {
            val intentDebugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)?.trim()
            val savedHarnessToken = metaPrefs.getString(KEY_HARNESS_TOKEN, null)?.trim()

            when {
                // Case 1: AI Studio emulator / test harness explicitly supplied a registered debug token
                !intentDebugToken.isNullOrEmpty() -> {
                    // Synchronous commit() ensures the secret is written before DebugAppCheckProviderFactory reads it.
                    debugStorePrefs.edit()
                        .putString(DEBUG_SECRET_KEY, intentDebugToken)
                        .commit()
                    metaPrefs.edit()
                        .putString(KEY_HARNESS_TOKEN, intentDebugToken)
                        .commit()
                    FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                        DebugAppCheckProviderFactory.getInstance()
                    )
                    Log.d(TAG, "App Check debug secret configured from launch Intent; DebugAppCheckProviderFactory installed.")
                }

                // Case 2: Re-opened inside AI Studio emulator where a registered harness token was previously injected
                !savedHarnessToken.isNullOrEmpty() -> {
                    debugStorePrefs.edit()
                        .putString(DEBUG_SECRET_KEY, savedHarnessToken)
                        .commit()
                    FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                        DebugAppCheckProviderFactory.getInstance()
                    )
                    Log.d(TAG, "App Check debug secret restored from harness cache; DebugAppCheckProviderFactory installed.")
                }

                // Case 3: Installed on a real Android device via APK (no harness debug token provided).
                // Clear any stale unregistered random UUID and use the provisioned reCAPTCHA Enterprise provider
                // so real phones receive valid Firebase App Check tokens instead of HTTP 403 invalid-token errors.
                else -> {
                    debugStorePrefs.edit().remove(DEBUG_SECRET_KEY).commit()
                    if (!installRecaptchaProvider(context)) {
                        Log.w(TAG, "No recaptcha_site_key available; falling back to DebugAppCheckProviderFactory.")
                        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                            DebugAppCheckProviderFactory.getInstance()
                        )
                    }
                }
            }
        } else {
            installRecaptchaProvider(context)
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize Firebase App Check", e)
    }
}

private fun installRecaptchaProvider(context: Context): Boolean {
    val resId = context.resources.getIdentifier(
        RECAPTCHA_SITE_KEY_RES,
        "string",
        context.packageName
    )
    if (resId == 0) {
        Log.e(TAG, "recaptcha_site_key string resource not found in package.")
        return false
    }
    val siteKey = context.getString(resId).trim()
    if (siteKey.isBlank()) {
        Log.e(TAG, "recaptcha_site_key resource string is blank.")
        return false
    }
    FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
        RecaptchaAppCheckProviderFactory.getInstance(siteKey)
    )
    Log.d(TAG, "RecaptchaAppCheckProviderFactory installed.")
    return true
}
