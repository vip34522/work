package com.example.schedulewidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import org.json.JSONObject
import java.util.*

class WeekWidget : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        // 处理左右翻页广播
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
            
            // 获取本周一的日期
            val monday = ShiftCalc.addDays(ShiftCalc.mondayOf(today), offset * 7)
            val weekNames = listOf("日", "一", "二", "三", "四", "五", "六")

            // 动态生成 7 天的视图
            views.removeAllViews(R.id.ll_week_days)
            for (i in 0 until 7) {
                val d = ShiftCalc.addDays(monday, i)
                val c = Calendar.getInstance().apply { time = d }
                val isToday = ShiftCalc.isToday(d)
                
                val shift = if (data != null && me != null) ShiftCalc.calcShift(data, me, d) else null
                val shiftStr = ShiftCalc.shiftShort(shift)
                
                // 简单模拟颜色，真实项目里可以通过班次名字去查颜色
                val shiftColor = when(shiftStr) {
                    "休" -> "#EF4444"
                    "白" -> "#2563EB"
                    "夜" -> "#1E293B"
                    "二" -> "#0EA5E9"
                    else -> "#64748B"
                }

                val dayView = RemoteViews(context.packageName, R.layout.item_week_day)
                dayView.setTextViewText(R.id.item_week_name, weekNames[c.get(Calendar.DAY_OF_WEEK) - 1])
                dayView.setTextViewText(R.id.item_day_num, c.get(Calendar.DAY_OF_MONTH).toString())
                dayView.setTextViewText(R.id.item_shift, shiftStr)
                dayView.setTextColor(R.id.item_shift, Color.parseColor(shiftColor))
                
                // 当天高亮逻辑（更换背景颜色）
                if (isToday) {
                    dayView.setInt(R.id.item_day_num, "setBackgroundColor", Color.parseColor("#EEF2FF")) // 浅蓝底
                }

                views.addView(R.id.ll_week_days, dayView)
            }

            // 设置底部详情
            views.setTextViewText(R.id.tv_bottom_date, "${calToday.get(Calendar.MONTH) + 1}月${calToday.get(Calendar.DAY_OF_MONTH)}日\n星期${weekNames[calToday.get(Calendar.DAY_OF_WEEK) - 1]}")
            if (data != null && me != null) {
                val todayShift = ShiftCalc.calcShift(data, me, today)
                views.setTextViewText(R.id.tv_bottom_shift, "今日：${ShiftCalc.shiftShort(todayShift)}")
            }

            // 设置翻页点击事件
            val prevIntent = Intent(context, WeekWidget::class.java).apply { action = "ACTION_WEEK_PREV" }
            val prevPending = PendingIntent.getBroadcast(context, 1, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.btn_prev_week, prevPending)

            val nextIntent = Intent(context, WeekWidget::class.java).apply { action = "ACTION_WEEK_NEXT" }
            val nextPending = PendingIntent.getBroadcast(context, 2, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.btn_next_week, nextPending)

            // 点击主体打开 App
            val openIntent = Intent(context, MainActivity::class.java)
            val openPending = PendingIntent.getActivity(context, 3, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.tv_bottom_shift, openPending)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
