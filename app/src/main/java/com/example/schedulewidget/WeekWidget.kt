package com.example.schedulewidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.text.Html
import android.widget.RemoteViews
import java.util.*

class WeekWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_week)
            val data = WidgetDataStore.load(context)
            val me = data?.let { ShiftCalc.getMe(it) }
            
            val html = StringBuilder()
            val today = Date()
            val monday = ShiftCalc.mondayOf(today)
            val weekNames = listOf("一", "二", "三", "四", "五", "六", "日")

            html.append("<b>本周排班：</b><br><br>")

            for (i in 0 until 7) {
                val d = ShiftCalc.addDays(monday, i)
                val c = Calendar.getInstance().apply { time = d }
                val shift = if (data != null && me != null) ShiftCalc.calcShift(data, me, d) else null
                val shiftStr = ShiftCalc.shiftShort(shift)
                
                // 根据班次给文字上色
                val color = when(shiftStr) {
                    "休" -> "#EF4444" // 红色
                    "白" -> "#3B82F6" // 蓝色
                    "夜" -> "#1E293B" // 深灰
                    "二" -> "#0EA5E9" // 浅蓝
                    else -> "#64748B"
                }

                // 拼接 HTML： 周一 26日 【白】
                html.append("周${weekNames[i]} ${c.get(Calendar.DAY_OF_MONTH)}日 ")
                html.append("<font color='$color'><b>【${shiftStr}】</b></font>")
                
                // 每隔一天换行或者加空格
                if (i == 3) html.append("<br><br>") else html.append("&nbsp;&nbsp;&nbsp;")
            }

            if (data != null && me != null) {
                val todayShift = ShiftCalc.calcShift(data, me, today)
                html.append("<br><br><b>今天（${Calendar.getInstance().get(Calendar.DAY_OF_MONTH)}日）:</b> <font color='#EF4444'>${ShiftCalc.shiftShort(todayShift)}</font>")
            }

            // 将 HTML 转换为带样式的文本
            views.setTextViewText(R.id.tv_week_html, Html.fromHtml(html.toString(), Html.FROM_HTML_MODE_LEGACY))

            // 点击打开 App
            val openIntent = Intent(context, MainActivity::class.java)
            val openPending = PendingIntent.getActivity(context, 3, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.tv_week_html, openPending)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
