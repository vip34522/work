package com.example.schedulewidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.util.*

class MonthWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_month)
            
            val data = WidgetDataStore.load(context)
            val me = data?.let { ShiftCalc.getMe(it) }
            
            val titleText = StringBuilder()
            val contentText = StringBuilder()

            if (me != null && data != null) {
                val cal = Calendar.getInstance()
                val y = cal.get(Calendar.YEAR)
                val m = cal.get(Calendar.MONTH) + 1
                titleText.append("${y}年${m}月 排班\n\n")
                
                val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                for (d in 1..daysInMonth) {
                    val date = Calendar.getInstance().apply { 
                        set(Calendar.YEAR, y)
                        set(Calendar.MONTH, m - 1)
                        set(Calendar.DAY_OF_MONTH, d)
                    }.time
                    
                    val shift = ShiftCalc.calcShift(data, me, date)
                    val shiftStr = ShiftCalc.shiftShort(shift)
                    
                    // 格式化：2日:白 4日:休
                    val dayStr = if (d < 10) " $d" else "$d"
                    contentText.append("${dayStr}日:${shiftStr}  ")
                    
                    // 每 5 天换行一次
                    if (d % 5 == 0) contentText.append("\n")
                }
            } else {
                titleText.append("暂无排班数据")
                contentText.append("请打开App同步数据")
            }

            views.setTextViewText(R.id.tv_month_title, titleText.toString() + contentText.toString())

            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.tv_month_title, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
