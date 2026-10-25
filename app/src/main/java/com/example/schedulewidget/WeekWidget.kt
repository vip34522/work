package com.example.schedulewidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
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

            // 1. 设置左侧日期
            views.setTextViewText(R.id.tv_date_day, "${cal.get(Calendar.MONTH) + 1}月${cal.get(Calendar.DAY_OF_MONTH)}日")
            views.setTextViewText(R.id.tv_date_week, "星期${listOf("日", "一", "二", "三", "四", "五", "六")[cal.get(Calendar.DAY_OF_WEEK) - 1]}")
            views.setTextViewText(R.id.tv_date_lunar, LunarCalc.lunarShort(today))

            // 2. 设置右侧本人信息
            if (me != null && data != null) {
                val typeId = me.optString("shiftTypeId")
                val types = data.optJSONArray("shiftTypes")
                var typeName = "排班"
                if (types != null) {
                    for (i in 0 until types.length()) {
                        if (types.getJSONObject(i).optString("id") == typeId) {
                            typeName = types.getJSONObject(i).optString("name")
                            break
                        }
                    }
                }
                val teamName = if (me.optString("team").isNotEmpty()) " · ${me.optString("team")}组" else ""
                views.setTextViewText(R.id.tv_person_info, "$typeName$teamName")

                val shift = ShiftCalc.calcShift(data, me, today)
                val shiftShort = ShiftCalc.shiftShort(shift)

                views.setTextViewText(R.id.tv_shift_big, shiftShort)

                // 使用最安全的纯色背景（不支持圆角）
                val bgColor = when (shiftShort) {
                    "休" -> Color.parseColor("#EF4444")
                    "白" -> Color.parseColor("#2563EB")
                    "夜" -> Color.parseColor("#1E293B")
                    "二" -> Color.parseColor("#0EA5E9")
                    else -> Color.parseColor("#64748B")
                }
                views.setInt(R.id.tv_shift_big, "setBackgroundColor", bgColor)
            } else {
                views.setTextViewText(R.id.tv_person_info, "请打开App同步")
                views.setTextViewText(R.id.tv_shift_big, "?")
            }

            // 3. 点击打开 App
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.tv_shift_big, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
