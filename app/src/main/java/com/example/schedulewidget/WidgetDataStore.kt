package com.example.schedulewidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import org.json.JSONObject

object WidgetDataStore {
    private const val PREFS = "widget_data"
    private const val KEY = "schedule_json"
    private const val KEY_WEEK_OFFSET = "week_offset"
    private const val KEY_MONTH_OFFSET = "month_offset"

    fun save(context: Context, json: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, json).apply()
        refreshWidgets(context)
    }

    fun load(context: Context): JSONObject? {
        val s = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return null
        return try { JSONObject(s) } catch (e: Exception) { null }
    }

    private fun refreshWidgets(context: Context) {
        val weekIntent = Intent(context, WeekWidget::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, WeekWidget::class.java))
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        context.sendBroadcast(weekIntent)

        val monthIntent = Intent(context, MonthWidget::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, MonthWidget::class.java))
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        context.sendBroadcast(monthIntent)
    }

    // --- 周视图偏移量 ---
    fun getWeekOffset(context: Context): Int = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_WEEK_OFFSET, 0)
    
    fun addWeekOffset(context: Context, delta: Int) {
        val v = getWeekOffset(context) + delta
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY_WEEK_OFFSET, v).apply()
    }

    // 新增：直接设置周偏移量（点击“今天”时清零）
    fun setWeekOffset(context: Context, offset: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY_WEEK_OFFSET, offset).apply()
    }

    // --- 月视图偏移量 ---
    fun getMonthOffset(context: Context): Int = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_MONTH_OFFSET, 0)
    
    fun addMonthOffset(context: Context, delta: Int) {
        val v = getMonthOffset(context) + delta
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY_MONTH_OFFSET, v).apply()
    }

    // 新增：直接设置月偏移量（点击“今天”时清零）
    fun setMonthOffset(context: Context, offset: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY_MONTH_OFFSET, offset).apply()
    }
}
