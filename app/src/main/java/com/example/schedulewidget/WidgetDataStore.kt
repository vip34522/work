package com.example.schedulewidget

import android.content.Context
import org.json.JSONObject

object WidgetDataStore {
    private const val PREFS = "widget_data"
    private const val KEY = "schedule_json"
    private const val KEY_WEEK_OFFSET = "week_offset"
    private const val KEY_MONTH_OFFSET = "month_offset"

    fun save(context: Context, json: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, json).apply()
        WeekWidgetReceiver.refresh(context)
        MonthWidgetReceiver.refresh(context)
    }

    fun load(context: Context): JSONObject? {
        val s = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return null
        return try { JSONObject(s) } catch (e: Exception) { null }
    }

    fun getWeekOffset(context: Context): Int = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_WEEK_OFFSET, 0)
    fun addWeekOffset(context: Context, delta: Int) {
        val v = getWeekOffset(context) + delta
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY_WEEK_OFFSET, v).apply()
    }

    fun getMonthOffset(context: Context): Int = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_MONTH_OFFSET, 0)
    fun addMonthOffset(context: Context, delta: Int) {
        val v = getMonthOffset(context) + delta
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY_MONTH_OFFSET, v).apply()
    }
}
