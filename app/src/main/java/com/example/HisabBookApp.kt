package com.example

import android.app.Application
import com.example.data.db.HisabDatabase
import com.example.data.prefs.SettingsDataStore
import com.example.data.repo.HisabRepository

class HisabBookApp : Application() {
    val database: HisabDatabase by lazy { HisabDatabase.getInstance(this) }
    val repository: HisabRepository by lazy { HisabRepository(this, database) }
    val settingsStore: SettingsDataStore by lazy { SettingsDataStore(this) }
}
