package com.example.schedulewidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
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
            
            val today = Date()
            val calToday = Calendar.getInstance().apply { time = today }
            val weekNames = listOf("日", "一", "二", "三", "四", "五", "六")
            
            views.setTextViewText(R.id.tv_header_month, "${calToday.get(Calendar.MONTH) + 1}月")
            views.setTextViewText(R.id.tv_header_day, "${calToday.get(Calendar.DAY_OF_MONTH)}日\n周${weekNames[calToday.get(Calendar.DAY_OF_WEEK) - 1]}")

            val monday = ShiftCalc.addDays(ShiftCalc.mondayOf(today), offset * 7)

            // 写死 ID 数组，100% 不会冲突
            val wIds = intArrayOf(R.id.tv_w0, R.id.tv_w1, R.id.tv_w2, R.id.tv_w3, R.id.tv_w4, R.id.tv_w5, R.id.tv_w6)
            val dIds = intArrayOf(R.id.tv_d0, R.id.tv_d1, R.id.tv_d2, R.id.tv_d3, R.id.tv_d4, R.id.tv_d5, R.id.tv_d6)
            val sIds = intArrayOf(R.id.tv_s0, R.id.tv_s1, R.id.tv_s2, R.id.tv_s3, R.id.tv_s4, R.id.tv_s5, R.id.tv_s6)

            for (i in 0 until 7) {
                val d = ShiftCalc.addDays(monday, i)
                val c = Calendar.getInstance().apply { time = d }
                val isToday = ShiftCalc.isToday(d)
                
                val shift = if (data != null && me != null) ShiftCalc.calcShift(data, me, d) else null
                val shiftStr = ShiftCalc.shiftShort(shift)

                views.setTextViewText(wIds[i], weekNames[c.get(Calendar.DAY_OF_WEEK) - 1])
                views.setTextViewText(dIds[i], c.get(Calendar.DAY_OF_MONTH).toString())
                views.setTextViewText(sIds[i], shiftStr)

                // 使用提前画好的安全圆角背景
                val bgResId = when(shiftStr) {
                    "白", "白班" -> R.drawable.bg_shift_white
                    "夜", "夜班" -> R.drawable.bg_shift_night
                    "休", "休息" -> R.drawable.bg_shift_rest
                    else -> R.drawable.bg_shift_other
                }
                views.setInt(sIds[i], "setBackgroundResource", bgResId)
                
                // 今天的高亮
                if (isToday) {
                    views.setInt(dIds[i], "setBackgroundResource", R.drawable.bg_shift_white) // 简单用个蓝底
                }
            }

            if (data != null && me != null) {
                val todayShift = ShiftCalc.calcShift(data, me, today)
                views.setTextViewText(R.id.tv_bottom_shift, "今天·本人 ${if (todayShift == null) "休息" else todayShift}")
            }

            // 翻页
            val prevIntent = Intent(context, WeekWidget::class.java).apply { action = "ACTION_WEEK_PREV" }
            val prevPending = PendingIntent.getBroadcast(context, 1, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.btn_prev_week, prevPending)

            val nextIntent = Intent(context, WeekWidget::class.java).apply { action = "ACTION_WEEK_NEXT" }
            val nextPending = PendingIntent.getBroadcast(context, 2, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.btn_next_week, nextPending)

            val openIntent = Intent(context, MainActivity::class.java)
            val openPending = PendingIntent.getActivity(context, 3, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.tv_bottom_shift, openPending)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
