package com.blackbotek.app1

import android.content.SharedPreferences
import java.util.UUID

/**
 * Objeto centralizado para gestionar el ID único del usuario.
 * Evita la duplicación de lógica entre LoginActivity y MainActivity.
 */
object UserSession {
    fun getOrCreateId(prefs: SharedPreferences): String {
        return prefs.getString("user_id", null) ?: run {
            val nuevo = "BOT-" + UUID.randomUUID().toString().substring(0, 8).uppercase()
            prefs.edit().putString("user_id", nuevo).apply()
            nuevo
        }
    }

    fun getId(prefs: SharedPreferences): String? =
        prefs.getString("user_id", null)

    fun clear(prefs: SharedPreferences) {
        prefs.edit().remove("user_id").apply()
    }
}
