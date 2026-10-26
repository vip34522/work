package com.example.schedulewidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.RemoteViews

class MonthWidget : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "ACTION_MONTH_PREV") {
            WidgetDataStore.addMonthOffset(context, -1)
            updateAll(context)
        } else if (intent.action == "ACTION_MONTH_NEXT") {
            WidgetDataStore.addMonthOffset(context, 1)
            updateAll(context)
        } else if (intent.action == "ACTION_MONTH_TODAY") {
            WidgetDataStore.setMonthOffset(context, 0)
            updateAll(context)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateAll(context)
    }

    private fun updateAll(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(android.content.ComponentName(context, MonthWidget::class.java))
        
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_month)
            val data = WidgetDataStore.load(context)
            val me = data?.let { ShiftCalc.getMe(it) }
            val offset = WidgetDataStore.getMonthOffset(context)
            
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            var minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            var minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
            
            if (minWidth <= 0) minWidth = 320
            // 【核心修复】如果系统给的高度小于 380，强制设为 380，保证 6 行绝不被挤压
            if (minHeight < 380) minHeight = 380 

            val density = context.resources.displayMetrics.density
            val widthPx = (minWidth * density).toInt()
            var heightPx = ((minHeight - 56) * density).toInt()

            try {
                val bitmap = MonthBitmapGenerator.generateMonthBitmap(widthPx, heightPx, data, me, offset)
                views.setImageViewBitmap(R.id.iv_month_canvas, bitmap)
            } catch (e: Exception) {
                val empty = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
                views.setImageViewBitmap(R.id.iv_month_canvas, empty)
            }

            val prevIntent = Intent(context, MonthWidget::class.java).apply { action = "ACTION_MONTH_PREV" }
            val prevPending = PendingIntent.getBroadcast(context, 11, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.btn_prev_month, prevPending)

            val todayIntent = Intent(context, MonthWidget::class.java).apply { action = "ACTION_MONTH_TODAY" }
            val todayPending = PendingIntent.getBroadcast(context, 14, todayIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.btn_today_month, todayPending)

            val nextIntent = Intent(context, MonthWidget::class.java).apply { action = "ACTION_MONTH_NEXT" }
            val nextPending = PendingIntent.getBroadcast(context, 12, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.btn_next_month, nextPending)

            val openIntent = Intent(context, MainActivity::class.java)
            val openPending = PendingIntent.getActivity(context, 13, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.iv_month_canvas, openPending)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
