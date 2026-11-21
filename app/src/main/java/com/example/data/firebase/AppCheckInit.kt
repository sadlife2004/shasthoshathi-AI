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

private const val APP_CHECK_MODE_PREFS = "shastho_appcheck_mode_prefs"
private const val PREF_USE_CUSTOM_DEBUG = "use_custom_debug_token"

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
        val modePrefs = context.getSharedPreferences(APP_CHECK_MODE_PREFS, Context.MODE_PRIVATE)
        val appCheck = FirebaseAppCheck.getInstance()

        // 1. AI Studio Streaming Emulator / CI Harness path: intent carries pre-registered debug token.
        if (BuildConfig.DEBUG && !intentDebugToken.isNullOrEmpty()) {
            // CRITICAL: MUST use synchronous commit() before DebugAppCheckProviderFactory reads it.
            debugStorePrefs.edit()
                .putString(DEBUG_SECRET_KEY, intentDebugToken)
                .commit()
            AppCheckDiagnostics.currentDebugSecret = intentDebugToken
            appCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            appCheck.setTokenAutoRefreshEnabled(true)
            AppCheckDiagnostics.activeProvider = "DebugAppCheckProvider"
            isAppCheckInstalled.set(true)
            Log.d(TAG, "DebugAppCheckProviderFactory installed with launch Intent token.")
            return
        }

        // 2. Developer opt-in path: user explicitly applied a Firebase Console allow-listed debug token.
        val useCustomDebug = BuildConfig.DEBUG && modePrefs.getBoolean(PREF_USE_CUSTOM_DEBUG, false)
        val existingDebugSecret = debugStorePrefs.getString(DEBUG_SECRET_KEY, null)?.trim()
        if (useCustomDebug && !existingDebugSecret.isNullOrEmpty()) {
            AppCheckDiagnostics.currentDebugSecret = existingDebugSecret
            appCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            appCheck.setTokenAutoRefreshEnabled(true)
            AppCheckDiagnostics.activeProvider = "DebugAppCheckProvider"
            isAppCheckInstalled.set(true)
            Log.d(TAG, "DebugAppCheckProviderFactory installed with user allow-listed token.")
            return
        }

        // 3. Real Android Device default (both Release and sideloaded Debug APKs without launch intent token):
        // Use reCAPTCHA Enterprise directly so real phones work automatically without manual Firebase Console
        // debug-token allow-listing. Never wipe cached tokens or force-refresh in a loop.
        val siteKey = resolveRecaptchaSiteKey(context)
        if (!siteKey.isNullOrBlank()) {
            appCheck.installAppCheckProviderFactory(
                RecaptchaAppCheckProviderFactory.getInstance(siteKey)
            )
            appCheck.setTokenAutoRefreshEnabled(true)
            AppCheckDiagnostics.activeProvider = "reCAPTCHA Enterprise"
            isAppCheckInstalled.set(true)
            Log.d(TAG, "RecaptchaAppCheckProviderFactory installed (automatic device attestation).")
            return
        }

        // 4. Fallback if recaptcha_site_key is unavailable in a debug build.
        if (BuildConfig.DEBUG) {
            val activeSecret = if (!existingDebugSecret.isNullOrEmpty()) {
                existingDebugSecret
            } else {
                val generated = UUID.randomUUID().toString()
                debugStorePrefs.edit()
                    .putString(DEBUG_SECRET_KEY, generated)
                    .commit()
                generated
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
            Log.d(TAG, "DebugAppCheckProviderFactory installed (fallback).")
        } else {
            Log.e(TAG, "recaptcha_site_key string resource not found or blank in package.")
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize Firebase App Check", e)
    }
}

private fun resolveRecaptchaSiteKey(context: Context): String? {
    val resId = context.resources.getIdentifier(
        RECAPTCHA_SITE_KEY_RES,
        "string",
        context.packageName
    )
    if (resId == 0) return null
    return context.getString(resId).trim().takeIf { it.isNotBlank() }
}

/**
 * Returns the active Firebase App Check debug secret UUID stored on this device (for optional Firebase Console allow-listing).
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
 * enables custom debug mode, and reinstalls DebugAppCheckProviderFactory.
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
        val modePrefs = context.getSharedPreferences(APP_CHECK_MODE_PREFS, Context.MODE_PRIVATE)
        debugStorePrefs.edit().putString(DEBUG_SECRET_KEY, cleaned).commit()
        tokenStorePrefs.edit().clear().commit()
        modePrefs.edit().putBoolean(PREF_USE_CUSTOM_DEBUG, true).commit()
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

/**
 * Switches back to automatic reCAPTCHA Enterprise attestation so no manual Firebase Console debug token is required.
 */
fun switchToAutomaticAppCheck(context: Context): Boolean {
    return try {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        val siteKey = resolveRecaptchaSiteKey(context) ?: return false
        val modePrefs = context.getSharedPreferences(APP_CHECK_MODE_PREFS, Context.MODE_PRIVATE)
        modePrefs.edit().putBoolean(PREF_USE_CUSTOM_DEBUG, false).commit()

        val appCheck = FirebaseAppCheck.getInstance()
        appCheck.installAppCheckProviderFactory(
            RecaptchaAppCheckProviderFactory.getInstance(siteKey)
        )
        appCheck.setTokenAutoRefreshEnabled(true)
        AppCheckDiagnostics.activeProvider = "reCAPTCHA Enterprise"
        isAppCheckInstalled.set(true)
        Log.d(TAG, "Switched back to RecaptchaAppCheckProviderFactory.")
        true
    } catch (e: Exception) {
        Log.e(TAG, "Failed to switch to automatic App Check", e)
        false
    }
}
