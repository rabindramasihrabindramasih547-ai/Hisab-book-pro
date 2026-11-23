package com.example.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.PaymentMethod
import com.example.ui.i18n.AppLanguage
import com.example.ui.theme.ThemeMode
import com.example.util.PinSecurity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hisabbook_settings")

data class AppSettings(
    val businessName: String = "My Business",
    val ownerName: String = "",
    val ownerMobile: String = "",
    val businessLogoPath: String? = null,
    val language: AppLanguage = AppLanguage.HINDI,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val defaultPaymentMethod: PaymentMethod = PaymentMethod.CASH,
    val pinEnabled: Boolean = false,
    val pinHash: String = "",
    val pinSalt: String = "",
    val biometricEnabled: Boolean = false,
    val pendingPaymentAlertsEnabled: Boolean = true,
    val backupReminderEnabled: Boolean = true,
    val lastBackupTimestamp: Long = 0L
)

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val BUSINESS_NAME = stringPreferencesKey("business_name")
        val OWNER_NAME = stringPreferencesKey("owner_name")
        val OWNER_MOBILE = stringPreferencesKey("owner_mobile")
        val BUSINESS_LOGO = stringPreferencesKey("business_logo_path")
        val LANGUAGE = stringPreferencesKey("language")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEFAULT_PAYMENT_METHOD = stringPreferencesKey("default_payment_method")
        val PIN_ENABLED = booleanPreferencesKey("pin_enabled")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val PENDING_ALERTS = booleanPreferencesKey("pending_payment_alerts")
        val BACKUP_REMINDER = booleanPreferencesKey("backup_reminder")
        val LAST_BACKUP_TS = longPreferencesKey("last_backup_ts")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            businessName = prefs[Keys.BUSINESS_NAME] ?: "HisabBook Business",
            ownerName = prefs[Keys.OWNER_NAME] ?: "",
            ownerMobile = prefs[Keys.OWNER_MOBILE] ?: "",
            businessLogoPath = prefs[Keys.BUSINESS_LOGO],
            language = when (prefs[Keys.LANGUAGE]) {
                AppLanguage.ENGLISH.name -> AppLanguage.ENGLISH
                else -> AppLanguage.HINDI
            },
            themeMode = when (prefs[Keys.THEME_MODE]) {
                ThemeMode.LIGHT.name -> ThemeMode.LIGHT
                ThemeMode.DARK.name -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            },
            defaultPaymentMethod = PaymentMethod.fromCode(
                prefs[Keys.DEFAULT_PAYMENT_METHOD] ?: PaymentMethod.CASH.code
            ),
            pinEnabled = prefs[Keys.PIN_ENABLED] ?: false,
            pinHash = prefs[Keys.PIN_HASH] ?: "",
            pinSalt = prefs[Keys.PIN_SALT] ?: "",
            biometricEnabled = prefs[Keys.BIOMETRIC_ENABLED] ?: false,
            pendingPaymentAlertsEnabled = prefs[Keys.PENDING_ALERTS] ?: true,
            backupReminderEnabled = prefs[Keys.BACKUP_REMINDER] ?: true,
            lastBackupTimestamp = prefs[Keys.LAST_BACKUP_TS] ?: 0L
        )
    }

    suspend fun updateBusinessProfile(
        businessName: String,
        ownerName: String,
        ownerMobile: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BUSINESS_NAME] = businessName.trim().ifEmpty { "HisabBook Business" }
            prefs[Keys.OWNER_NAME] = ownerName.trim()
            prefs[Keys.OWNER_MOBILE] = ownerMobile.trim()
        }
    }

    suspend fun updateBusinessLogo(path: String?) {
        context.dataStore.edit { prefs ->
            if (path == null) prefs.remove(Keys.BUSINESS_LOGO)
            else prefs[Keys.BUSINESS_LOGO] = path
        }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LANGUAGE] = language.name
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    suspend fun setDefaultPaymentMethod(method: PaymentMethod) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DEFAULT_PAYMENT_METHOD] = method.code
        }
    }

    suspend fun setPin(pin: String) {
        val salt = PinSecurity.generateSaltBase64()
        val hash = PinSecurity.hashPin(pin, salt)
        context.dataStore.edit { prefs ->
            prefs[Keys.PIN_ENABLED] = true
            prefs[Keys.PIN_SALT] = salt
            prefs[Keys.PIN_HASH] = hash
        }
    }

    suspend fun disablePin() {
        context.dataStore.edit { prefs ->
            prefs[Keys.PIN_ENABLED] = false
            prefs[Keys.PIN_SALT] = ""
            prefs[Keys.PIN_HASH] = ""
            prefs[Keys.BIOMETRIC_ENABLED] = false
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BIOMETRIC_ENABLED] = enabled
        }
    }

    suspend fun setReminderPreferences(pendingAlerts: Boolean, backupReminder: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PENDING_ALERTS] = pendingAlerts
            prefs[Keys.BACKUP_REMINDER] = backupReminder
        }
    }

    suspend fun markBackupCompleted(timestamp: Long = System.currentTimeMillis()) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_BACKUP_TS] = timestamp
        }
    }
}
