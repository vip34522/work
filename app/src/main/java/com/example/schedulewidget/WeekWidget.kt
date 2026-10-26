package com.example.schedulewidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.util.DisplayMetrics
import android.widget.RemoteViews
import java.util.*

class WeekWidget : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "ACTION_WEEK_PREV") {
            WidgetDataStore.addWeekOffset(context, -1)
            updateAll(context)
        } else if (intent.action == "ACTION_WEEK_NEXT") {
            WidgetDataStore.addWeekOffset(context, 1)
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
            
            // 根据屏幕密度动态计算一个合适的宽高用于画图
            val density = context.resources.displayMetrics.density
            val widthPx = (300 * density).toInt()
            val heightPx = (120 * density).toInt()

            // 核心：生成画好的图片
            val bitmap = WeekBitmapGenerator.generateWeekBitmap(widthPx, heightPx, data, me, offset)

            // 将 Bitmap 塞入 ImageView
            views.setImageViewBitmap(R.id.iv_week_canvas, bitmap)

            // 翻页事件
            val prevIntent = Intent(context, WeekWidget::class.java).apply { action = "ACTION_WEEK_PREV" }
            val prevPending = PendingIntent.getBroadcast(context, 1, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.btn_prev_week, prevPending)

            val nextIntent = Intent(context, WeekWidget::class.java).apply { action = "ACTION_WEEK_NEXT" }
            val nextPending = PendingIntent.getBroadcast(context, 2, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.btn_next_week, nextPending)

            // 点击图片或标题打开 App
            val openIntent = Intent(context, MainActivity::class.java)
            val openPending = PendingIntent.getActivity(context, 3, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.iv_week_canvas, openPending)
            views.setOnClickPendingIntent(R.id.tv_header, openPending)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
