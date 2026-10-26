package com.example.schedulewidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.RemoteViews

class WeekWidget : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "ACTION_WEEK_PREV") {
            WidgetDataStore.addWeekOffset(context, -1)
            updateAll(context)
        } else if (intent.action == "ACTION_WEEK_NEXT") {
            WidgetDataStore.addWeekOffset(context, 1)
            updateAll(context)
        } else if (intent.action == "ACTION_WEEK_TODAY") {
            WidgetDataStore.setWeekOffset(context, 0) // 重置为 0
            updateAll(context)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateAll(context)
    }

    private fun updateAll(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(android.content.ComponentName(context, WeekWidget::class.java))
        
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_week)
            val data = WidgetDataStore.load(context)
            val me = data?.let { ShiftCalc.getMe(it) }
            val offset = WidgetDataStore.getWeekOffset(context)
            
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            var minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            var minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
            
            if (minWidth <= 0) minWidth = 250
            if (minHeight <= 0) minHeight = 150

            val density = context.resources.displayMetrics.density
            val widthPx = (minWidth * density).toInt()
            val heightPx = (minHeight * density).toInt()

            try {
                val bitmap = WeekBitmapGenerator.generateWeekBitmap(widthPx, heightPx, "排班系统", data, me, offset)
                views.setImageViewBitmap(R.id.iv_week_canvas, bitmap)
            } catch (e: Exception) {
                val empty = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
                views.setImageViewBitmap(R.id.iv_week_canvas, empty)
            }

            val prevIntent = Intent(context, WeekWidget::class.java).apply { action = "ACTION_WEEK_PREV" }
            val prevPending = PendingIntent.getBroadcast(context, 1, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.btn_prev_week, prevPending)

            val todayIntent = Intent(context, WeekWidget::class.java).apply { action = "ACTION_WEEK_TODAY" }
            val todayPending = PendingIntent.getBroadcast(context, 4, todayIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.btn_today_week, todayPending)

            val nextIntent = Intent(context, WeekWidget::class.java).apply { action = "ACTION_WEEK_NEXT" }
            val nextPending = PendingIntent.getBroadcast(context, 2, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.btn_next_week, nextPending)

            val openIntent = Intent(context, MainActivity::class.java)
            val openPending = PendingIntent.getActivity(context, 3, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.iv_week_canvas, openPending)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
