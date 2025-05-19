package com.example.foodtracker.modelos

import android.content.Context
import android.preference.PreferenceManager

object GuardarNotificaciones {

    private const val PREF_KEY = "notification_log"

    fun saveNotification(context: Context, message: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val current = prefs.getStringSet(PREF_KEY, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        current.add(message)
        prefs.edit().putStringSet(PREF_KEY, current).apply()
    }

    fun getNotifications(context: Context): List<String> {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getStringSet(PREF_KEY, mutableSetOf())?.toList()?.sortedByDescending { it } ?: listOf()
    }
}