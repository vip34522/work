package com.example.schedulewidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
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
            
            if (minWidth <= 0) minWidth = 300
            if (minHeight <= 0) minHeight = 300

            val density = context.resources.displayMetrics.density
            val widthPx = (minWidth * density).toInt()
            val heightPx = (minHeight * density).toInt()

            // 传入了"排班系统"文字，以后想改直接改这
            val bitmap = MonthBitmapGenerator.generateMonthBitmap(widthPx, heightPx, "排班系统", data, me, offset)

            views.setImageViewBitmap(R.id.iv_month_canvas, bitmap)

            // 翻页交互
            val prevIntent = Intent(context, MonthWidget::class.java).apply { action = "ACTION_MONTH_PREV" }
            val prevPending = PendingIntent.getBroadcast(context, 11, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.btn_prev_month, prevPending)

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
