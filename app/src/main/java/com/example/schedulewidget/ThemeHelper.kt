package com.example.schedulewidget

import android.content.Context
import org.json.JSONObject

object ThemeHelper {
    data class ThemeColors(val primary: Int, val isDark: Boolean)

    fun get(context: Context): ThemeColors {
        val data = WidgetDataStore.load(context)
        val primary = parsePrimary(data)
        val isDark = try { data?.optJSONObject("theme")?.optBoolean("dark", false) ?: false } catch (e: Exception) { false }
        return ThemeColors(primary, isDark)
    }

    private fun parsePrimary(data: JSONObject?): Int {
        val default = 0xFF2563EB.toInt()
        if (data == null) return default
        val theme = try { data.optJSONObject("theme") } catch (e: Exception) { null } ?: return default
        return when (val p = theme.opt("primary")) {
            is JSONObject -> {
                val type = p.optString("type", "solid")
                val hex = when (type) { "solid" -> p.optString("color", "#2563eb"); "linear2", "linear3" -> p.optString("c1", "#2563eb"); else -> "#2563eb" }
                parseHex(hex)
            }
            is String -> parseHex(p)
            else -> default
        }
    }

    private fun parseHex(hex: String): Int {
        val h = hex.replace("#", "").trim()
        return try {
            when (h.length) {
                6 -> (0xFF000000.toInt() or h.toLong(16).toInt())
                3 -> 0xFF000000.toInt() or h.map { "$it$it" }.joinToString("").toLong(16).toInt()
                else -> 0xFF2563EB.toInt()
            }
        } catch (e: Exception) { 0xFF2563EB.toInt() }
    }
}
