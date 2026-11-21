package com.example.data.firebase

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "AppCheckInit"

private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val TOKEN_STORE_PREFIX = "com.google.firebase.appcheck.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

// Dedicated metadata store to distinguish a harness-injected registered debug token
// (e.g., AI Studio emulator / CI) from an unregistered random UUID on a real phone,
// and to run stale-token cleanup strictly once across app upgrades.
private const val META_PREFS = "shasthosathi_appcheck_meta"
private const val KEY_HARNESS_TOKEN = "harness_debug_token"
private const val KEY_MIGRATED_V12 = "migrated_clean_recaptcha_v12"

object AppCheckDiagnostics {
    @Volatile
    var activeProvider: String = "Uninitialized"
}

private val isAppCheckInstalled = AtomicBoolean(false)

fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }

        val intentDebugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)?.trim()

        // Avoid re-installing App Check provider on Activity recreations (e.g. rotation, sign-in return, camera),
        // unless a fresh harness debug token arrived via launch Intent.
        if (isAppCheckInstalled.get() && intentDebugToken.isNullOrEmpty()) {
            return
        }

        val firebaseApp = FirebaseApp.getInstance()
        val persistenceKey = firebaseApp.persistenceKey
        val debugStorePrefs = context.getSharedPreferences(
            "$DEBUG_STORE_PREFIX$persistenceKey",
            Context.MODE_PRIVATE
        )
        val tokenStorePrefs = context.getSharedPreferences(
            "$TOKEN_STORE_PREFIX$persistenceKey",
            Context.MODE_PRIVATE
        )
        val metaPrefs = context.getSharedPreferences(META_PREFS, Context.MODE_PRIVATE)
        val appCheck = FirebaseAppCheck.getInstance()

        if (BuildConfig.DEBUG) {
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
                    appCheck.installAppCheckProviderFactory(
                        DebugAppCheckProviderFactory.getInstance()
                    )
                    appCheck.setTokenAutoRefreshEnabled(true)
                    AppCheckDiagnostics.activeProvider = "Debug (Emulator)"
                    isAppCheckInstalled.set(true)
                    Log.d(TAG, "App Check debug secret configured from launch Intent; DebugAppCheckProviderFactory installed.")
                }

                // Case 2: Re-opened inside AI Studio emulator where a registered harness token was previously injected
                !savedHarnessToken.isNullOrEmpty() -> {
                    debugStorePrefs.edit()
                        .putString(DEBUG_SECRET_KEY, savedHarnessToken)
                        .commit()
                    appCheck.installAppCheckProviderFactory(
                        DebugAppCheckProviderFactory.getInstance()
                    )
                    appCheck.setTokenAutoRefreshEnabled(true)
                    AppCheckDiagnostics.activeProvider = "Debug (Emulator Cached)"
                    isAppCheckInstalled.set(true)
                    Log.d(TAG, "App Check debug secret restored from harness cache; DebugAppCheckProviderFactory installed.")
                }

                // Case 3: Installed directly on a real Android phone via APK (no harness debug token provided).
                // Clear stale unregistered debug tokens ONCE, then install reCAPTCHA Enterprise and let
                // Firebase SDK fetch & cache tokens naturally on demand (never call forceRefresh in onCreate).
                else -> {
                    if (!metaPrefs.getBoolean(KEY_MIGRATED_V12, false)) {
                        debugStorePrefs.edit().clear().commit()
                        tokenStorePrefs.edit().clear().commit()
                        metaPrefs.edit().putBoolean(KEY_MIGRATED_V12, true).commit()
                    }
                    if (!installRecaptchaProvider(context, appCheck)) {
                        Log.w(TAG, "No recaptcha_site_key available; falling back to DebugAppCheckProviderFactory.")
                        appCheck.installAppCheckProviderFactory(
                            DebugAppCheckProviderFactory.getInstance()
                        )
                        appCheck.setTokenAutoRefreshEnabled(true)
                        AppCheckDiagnostics.activeProvider = "Debug (Fallback)"
                    }
                    isAppCheckInstalled.set(true)
                }
            }
        } else {
            installRecaptchaProvider(context, appCheck)
            isAppCheckInstalled.set(true)
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize Firebase App Check", e)
    }
}

private fun installRecaptchaProvider(context: Context, appCheck: FirebaseAppCheck): Boolean {
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
    appCheck.installAppCheckProviderFactory(
        RecaptchaAppCheckProviderFactory.getInstance(siteKey)
    )
    appCheck.setTokenAutoRefreshEnabled(true)
    AppCheckDiagnostics.activeProvider = "reCAPTCHA Enterprise"
    Log.d(TAG, "RecaptchaAppCheckProviderFactory installed once (key=${siteKey.take(8)}...).")
    return true
}
