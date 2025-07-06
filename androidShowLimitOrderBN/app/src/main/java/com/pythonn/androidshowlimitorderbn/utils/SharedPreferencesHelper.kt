package com.pythonn.androidshowlimitorderbn.utils

import android.content.Context
import android.content.SharedPreferences

class SharedPreferencesHelper(context: Context) {

    private val PREFS_NAME = "app_prefs"
    private val KEY_THRESHOLD = "key_threshold"

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveThreshold(threshold: Double) {
        sharedPreferences.edit().putFloat(KEY_THRESHOLD, threshold.toFloat()).apply()
    }

    fun getThreshold(): Double {
        return sharedPreferences.getFloat(KEY_THRESHOLD, 50.0f).toDouble() // Default to 50.0
    }
}