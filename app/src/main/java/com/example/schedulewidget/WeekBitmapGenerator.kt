package com.example.schedulewidget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import java.util.*

object WeekBitmapGenerator {

    fun generateWeekBitmap(width: Int, height: Int, data: org.json.JSONObject?, me: org.json.JSONObject?, offset: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE) // 整体白底

        val today = Date()
        val monday = ShiftCalc.addDays(ShiftCalc.mondayOf(today), offset * 7)
        val weekNames = listOf("一", "二", "三", "四", "五", "六", "日")

        val cellWidth = width / 7f
        val cellHeight = height / 1.5f // 留出底部空间

        // 定义画笔
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
        }

        for (i in 0 until 7) {
            val d = ShiftCalc.addDays(monday, i)
            val c = Calendar.getInstance().apply { time = d }
            val isToday = ShiftCalc.isToday(d)
            
            val shift = if (data != null && me != null) ShiftCalc.calcShift(data, me, d) else null
            val shiftStr = ShiftCalc.shiftShort(shift)
            
            // 确定颜色
            val shiftColor = when(shiftStr) {
                "休" -> "#EF4444" // 红
                "白" -> "#3B82F6" // 蓝
                "夜" -> "#1E293B" // 灰
                "二" -> "#0EA5E9" // 浅蓝
                else -> "#10B981" // 绿
            }

            val centerX = cellWidth * i + cellWidth / 2
            val startY = 10f
            
            // 1. 画星期几
            textPaint.color = Color.parseColor(if(isToday) "#2563EB" else "#64748B")
            textPaint.textSize = 30f
            textPaint.isFakeBoldText = isToday
            canvas.drawText("周${weekNames[i]}", centerX, startY + 30f, textPaint)

            // 2. 画日期数字
            textPaint.color = Color.parseColor("#1E293B")
            textPaint.textSize = 40f
            textPaint.isFakeBoldText = true
            canvas.drawText(c.get(Calendar.DAY_OF_MONTH).toString(), centerX, startY + 90f, textPaint)

            // 3. 画班次圆角色块
            val rectWidth = cellWidth * 0.7f
            val rectHeight = 50f
            val rectLeft = centerX - rectWidth / 2
            val rectTop = startY + 120f
            val rectRight = centerX + rectWidth / 2
            val rectBottom = rectTop + rectHeight

            bgPaint.color = Color.parseColor(shiftColor)
            val rectF = RectF(rectLeft, rectTop, rectRight, rectBottom)
            canvas.drawRoundRect(rectF, 16f, 16f, bgPaint) // 16f 是圆角大小

            // 4. 画色块里的字
            textPaint.color = Color.WHITE
            textPaint.textSize = 32f
            textPaint.isFakeBoldText = false
            // 文字垂直居中微调
            val textY = rectTop + (rectHeight / 2) - ((textPaint.descent() + textPaint.ascent()) / 2)
            canvas.drawText(shiftStr, centerX, textY, textPaint)
        }

        return bitmap
    }
}
