package com.example.schedulewidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.util.*

class WeekWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_week)
            val data = WidgetDataStore.load(context)
            val me = data?.let { ShiftCalc.getMe(it) }
            val today = Date()
            val cal = Calendar.getInstance().apply { time = today }

            // 1. 设置日期行
            val weekName = listOf("日", "一", "二", "三", "四", "五", "六")[cal.get(Calendar.DAY_OF_WEEK) - 1]
            val lunar = LunarCalc.lunarShort(today)
            val dateStr = "${cal.get(Calendar.MONTH) + 1}月${cal.get(Calendar.DAY_OF_MONTH)}日 星期$weekName $lunar"
            views.setTextViewText(R.id.tv_date, dateStr)

            // 2. 设置班次行
            if (me != null && data != null) {
                val shift = ShiftCalc.calcShift(data, me, today)
                val shiftShort = ShiftCalc.shiftShort(shift)
                views.setTextViewText(R.id.tv_shift, "今天班次: $shiftShort")
            } else {
                views.setTextViewText(R.id.tv_shift, "暂无数据，请打开App")
            }

            // 3. 点击打开 App
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.tv_shift, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
