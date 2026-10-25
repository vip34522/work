package com.example.schedulewidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class WeekWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_week)
            
            // 这里可以随时替换成读取 JSON 的真实逻辑
            views.setTextViewText(R.id.tv_week_title, "排班周视图")
            views.setTextViewText(R.id.tv_week_content, "周一: 白班 | 周二: 夜班 | 测试成功！")

            // 点击打开 App
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.tv_week_title, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
