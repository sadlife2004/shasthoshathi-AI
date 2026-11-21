package com.example.data.firebase

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "AppCheckInit"
private const val DEBUG_PROVIDER_LOG_TAG = "DebugAppCheckProvider"

private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val TOKEN_STORE_PREFIX = "com.google.firebase.appcheck.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

object AppCheckDiagnostics {
    @Volatile
    var activeProvider: String = "Uninitialized"

    @Volatile
    var currentDebugSecret: String? = null
}

private val isAppCheckInstalled = AtomicBoolean(false)

fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }

        val intentDebugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)?.trim()

        // Avoid re-installing App Check provider on Activity recreations unless a fresh harness token arrived.
        if (isAppCheckInstalled.get() && intentDebugToken.isNullOrEmpty()) {
            return
        }

        val firebaseApp = FirebaseApp.getInstance()
        val persistenceKey = firebaseApp.persistenceKey
        val debugStorePrefs = context.getSharedPreferences(
            "$DEBUG_STORE_PREFIX$persistenceKey",
            Context.MODE_PRIVATE
        )
        val appCheck = FirebaseAppCheck.getInstance()

        if (BuildConfig.DEBUG) {
            val activeSecret = when {
                !intentDebugToken.isNullOrEmpty() -> {
                    // Synchronous commit() ensures the secret is written before DebugAppCheckProviderFactory reads it.
                    debugStorePrefs.edit()
                        .putString(DEBUG_SECRET_KEY, intentDebugToken)
                        .commit()
                    Log.d(TAG, "App Check debug secret configured from launch Intent.")
                    intentDebugToken
                }
                else -> {
                    val existing = debugStorePrefs.getString(DEBUG_SECRET_KEY, null)?.trim()
                    if (!existing.isNullOrEmpty()) {
                        existing
                    } else {
                        val generated = UUID.randomUUID().toString()
                        debugStorePrefs.edit()
                            .putString(DEBUG_SECRET_KEY, generated)
                            .commit()
                        generated
                    }
                }
            }

            AppCheckDiagnostics.currentDebugSecret = activeSecret
            Log.d(
                DEBUG_PROVIDER_LOG_TAG,
                "Enter this debug secret into the allow list in the Firebase Console for your project: $activeSecret"
            )

            appCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            appCheck.setTokenAutoRefreshEnabled(true)
            AppCheckDiagnostics.activeProvider = "DebugAppCheckProvider"
            isAppCheckInstalled.set(true)
            Log.d(TAG, "DebugAppCheckProviderFactory installed.")
        } else {
            val resId = context.resources.getIdentifier(
                RECAPTCHA_SITE_KEY_RES,
                "string",
                context.packageName
            )
            if (resId != 0) {
                val siteKey = context.getString(resId).trim()
                if (siteKey.isNotBlank()) {
                    appCheck.installAppCheckProviderFactory(
                        RecaptchaAppCheckProviderFactory.getInstance(siteKey)
                    )
                    appCheck.setTokenAutoRefreshEnabled(true)
                    AppCheckDiagnostics.activeProvider = "reCAPTCHA Enterprise"
                    isAppCheckInstalled.set(true)
                    Log.d(TAG, "RecaptchaAppCheckProviderFactory installed.")
                } else {
                    Log.e(TAG, "recaptcha_site_key resource string is blank.")
                }
            } else {
                Log.e(TAG, "recaptcha_site_key string resource not found in package.")
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize Firebase App Check", e)
    }
}

/**
 * Returns the active Firebase App Check debug secret UUID stored on this device (Debug builds only).
 */
fun getCurrentAppCheckDebugSecret(context: Context): String {
    val cached = AppCheckDiagnostics.currentDebugSecret
    if (!cached.isNullOrBlank()) return cached
    return try {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        val persistenceKey = FirebaseApp.getInstance().persistenceKey
        val prefs = context.getSharedPreferences(
            "$DEBUG_STORE_PREFIX$persistenceKey",
            Context.MODE_PRIVATE
        )
        val existing = prefs.getString(DEBUG_SECRET_KEY, null)?.trim()
        if (!existing.isNullOrBlank()) {
            AppCheckDiagnostics.currentDebugSecret = existing
            existing
        } else {
            val generated = UUID.randomUUID().toString()
            prefs.edit().putString(DEBUG_SECRET_KEY, generated).commit()
            AppCheckDiagnostics.currentDebugSecret = generated
            generated
        }
    } catch (e: Exception) {
        Log.e(TAG, "Could not read debug secret", e)
        ""
    }
}

/**
 * Saves a user-provided Firebase Console App Check Debug Token synchronously,
 * clears any stale cached App Check token, and reinstalls DebugAppCheckProviderFactory.
 */
fun applyCustomAppCheckDebugSecret(context: Context, customToken: String): Boolean {
    val cleaned = customToken.trim()
    if (cleaned.length < 8) return false
    return try {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        val persistenceKey = FirebaseApp.getInstance().persistenceKey
        val debugStorePrefs = context.getSharedPreferences(
            "$DEBUG_STORE_PREFIX$persistenceKey",
            Context.MODE_PRIVATE
        )
        val tokenStorePrefs = context.getSharedPreferences(
            "$TOKEN_STORE_PREFIX$persistenceKey",
            Context.MODE_PRIVATE
        )
        debugStorePrefs.edit().putString(DEBUG_SECRET_KEY, cleaned).commit()
        tokenStorePrefs.edit().clear().commit()
        AppCheckDiagnostics.currentDebugSecret = cleaned

        val appCheck = FirebaseAppCheck.getInstance()
        appCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )
        appCheck.setTokenAutoRefreshEnabled(true)
        AppCheckDiagnostics.activeProvider = "DebugAppCheckProvider"
        isAppCheckInstalled.set(true)
        Log.d(
            DEBUG_PROVIDER_LOG_TAG,
            "Custom App Check debug secret applied and activated: $cleaned"
        )
        true
    } catch (e: Exception) {
        Log.e(TAG, "Failed to apply custom debug secret", e)
        false
    }
}
