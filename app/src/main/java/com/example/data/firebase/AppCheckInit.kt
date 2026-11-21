package com.example.data.firebase

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val TAG = "AppCheckInit"

private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val TOKEN_STORE_PREFIX = "com.google.firebase.appcheck.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

// Dedicated metadata store to distinguish a harness-injected registered debug token
// (e.g., AI Studio emulator / CI) from an unregistered random UUID generated on a real phone.
private const val META_PREFS = "shasthosathi_appcheck_meta"
private const val KEY_HARNESS_TOKEN = "harness_debug_token"

object AppCheckDiagnostics {
    @Volatile
    var activeProvider: String = "Uninitialized"

    @Volatile
    var lastTokenError: String? = null
}

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
        val tokenStorePrefs = context.getSharedPreferences(
            "$TOKEN_STORE_PREFIX$persistenceKey",
            Context.MODE_PRIVATE
        )
        val metaPrefs = context.getSharedPreferences(META_PREFS, Context.MODE_PRIVATE)
        val appCheck = FirebaseAppCheck.getInstance()

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
                    appCheck.installAppCheckProviderFactory(
                        DebugAppCheckProviderFactory.getInstance()
                    )
                    appCheck.setTokenAutoRefreshEnabled(true)
                    AppCheckDiagnostics.activeProvider = "Debug (Emulator)"
                    AppCheckDiagnostics.lastTokenError = null
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
                    AppCheckDiagnostics.lastTokenError = null
                    Log.d(TAG, "App Check debug secret restored from harness cache; DebugAppCheckProviderFactory installed.")
                }

                // Case 3: Installed directly on a real Android phone via APK (no harness debug token provided).
                // Clear both any stale unregistered random UUID and any stale cached App Check token,
                // then install the provisioned reCAPTCHA Enterprise provider and pre-warm it.
                else -> {
                    debugStorePrefs.edit().clear().commit()
                    tokenStorePrefs.edit().clear().commit()
                    if (!installRecaptchaProvider(context, appCheck)) {
                        Log.w(TAG, "No recaptcha_site_key available; falling back to DebugAppCheckProviderFactory.")
                        appCheck.installAppCheckProviderFactory(
                            DebugAppCheckProviderFactory.getInstance()
                        )
                        AppCheckDiagnostics.activeProvider = "Debug (Fallback)"
                    }
                }
            }
        } else {
            installRecaptchaProvider(context, appCheck)
        }
    } catch (e: Exception) {
        AppCheckDiagnostics.lastTokenError = e.localizedMessage ?: e.javaClass.simpleName
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
        AppCheckDiagnostics.lastTokenError = "recaptcha_site_key string resource not found."
        Log.e(TAG, "recaptcha_site_key string resource not found in package.")
        return false
    }
    val siteKey = context.getString(resId).trim()
    if (siteKey.isBlank()) {
        AppCheckDiagnostics.lastTokenError = "recaptcha_site_key resource string is blank."
        Log.e(TAG, "recaptcha_site_key resource string is blank.")
        return false
    }
    appCheck.installAppCheckProviderFactory(
        RecaptchaAppCheckProviderFactory.getInstance(siteKey)
    )
    appCheck.setTokenAutoRefreshEnabled(true)
    AppCheckDiagnostics.activeProvider = "reCAPTCHA Enterprise"
    AppCheckDiagnostics.lastTokenError = null
    Log.d(TAG, "RecaptchaAppCheckProviderFactory installed (key=${siteKey.take(8)}...). Pre-warming token...")

    // Pre-warm reCAPTCHA Enterprise attestation so the first AI request has a valid token ready
    appCheck.getAppCheckToken(true)
        .addOnSuccessListener {
            AppCheckDiagnostics.lastTokenError = null
            Log.d(TAG, "App Check reCAPTCHA token pre-warmed successfully.")
        }
        .addOnFailureListener { err ->
            AppCheckDiagnostics.lastTokenError = err.localizedMessage ?: err.javaClass.simpleName
            Log.e(TAG, "App Check reCAPTCHA token pre-warm failed", err)
        }
    return true
}

/**
 * Forces a fresh App Check token fetch (useful when retrying an AI call after a transient App Check failure).
 * Returns null if refreshed successfully, or an error description string if attestation failed.
 */
suspend fun forceRefreshAppCheckToken(): String? = suspendCancellableCoroutine { cont ->
    try {
        FirebaseAppCheck.getInstance()
            .getAppCheckToken(true)
            .addOnSuccessListener {
                AppCheckDiagnostics.lastTokenError = null
                Log.d(TAG, "Forced App Check token refresh succeeded (${AppCheckDiagnostics.activeProvider}).")
                if (cont.isActive) cont.resume(null)
            }
            .addOnFailureListener { err ->
                val msg = err.localizedMessage ?: err.javaClass.simpleName
                AppCheckDiagnostics.lastTokenError = msg
                Log.e(TAG, "Forced App Check token refresh failed (${AppCheckDiagnostics.activeProvider})", err)
                if (cont.isActive) cont.resume(msg)
            }
    } catch (e: Exception) {
        val msg = e.localizedMessage ?: e.javaClass.simpleName
        AppCheckDiagnostics.lastTokenError = msg
        Log.e(TAG, "Exception during forced App Check token refresh", e)
        if (cont.isActive) cont.resume(msg)
    }
}
