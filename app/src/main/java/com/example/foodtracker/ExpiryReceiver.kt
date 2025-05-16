package com.example.foodtracker

import NotificationHelper
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ExpiryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val nombre = intent.getStringExtra("nombreAlimento") ?: return
        NotificationHelper(context).showNotification(
            "¡Alimento caducado!",
            "$nombre ha caducado hoy. ¡Revisa tu despensa!"
        )
    }
}