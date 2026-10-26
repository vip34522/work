package com.example.schedulewidget

import android.graphics.*
import org.json.JSONObject
import java.util.*
import kotlin.math.ceil

object MonthBitmapGenerator {

    fun generateMonthBitmap(width: Int, height: Int, data: JSONObject?, me: JSONObject?, offset: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val cal = Calendar.getInstance().apply { add(Calendar.MONTH, offset) }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rectF = RectF()

        val w = width.toFloat()
        val h = height.toFloat()
        val paddingX = w * 0.03f

        fun getCenteredY(centerY: Float, paint: Paint): Float {
            val fm = paint.fontMetrics
            return centerY - (fm.ascent + fm.descent) / 2f
        }

        // 全白底
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, w, h, paint)

        val weekNames = listOf("一", "二", "三", "四", "五", "六", "日")
        val contentW = w - paddingX * 2
        val cellW = contentW / 7f
        val weekHeaderH = h * 0.08f 
        
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = Math.min(cellW * 0.35f, 30f)
        textPaint.color = Color.parseColor("#64748B")
        textPaint.isFakeBoldText = true

        for (i in 0 until 7) {
            val cx = paddingX + cellW * i + cellW / 2f
            canvas.drawText(weekNames[i], cx, getCenteredY(weekHeaderH / 2f, textPaint), textPaint)
        }

        val gridStartY = weekHeaderH
        // 【核心修复】去掉底部原本减去的 10f，让空间全部给网格
        val gridHeight = h - gridStartY 
        
        val firstDay = Calendar.getInstance().apply { set(year, month - 1, 1) }
        val startPad = if (firstDay.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) 6 else firstDay.get(Calendar.DAY_OF_WEEK) - 2
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val actualRows = ceil((startPad + daysInMonth) / 7.0).toInt()
        val cellH = gridHeight / actualRows 

        val todayCal = Calendar.getInstance()
        val isCurrentMonth = (todayCal.get(Calendar.YEAR) == year && todayCal.get(Calendar.MONTH) + 1 == month)
        val todayD = todayCal.get(Calendar.DAY_OF_MONTH)

        for (r in 0 until actualRows) {
            for (c in 0 until 7) {
                val dayIndex = r * 7 + c - startPad
                if (dayIndex < 0 || dayIndex >= daysInMonth) continue

                val d = dayIndex + 1
                val date = Calendar.getInstance().apply { set(year, month - 1, d) }.time
                val isToday = isCurrentMonth && d == todayD

                val left = paddingX + c * cellW
                val top = gridStartY + r * cellH
                val right = left + cellW
                val bottom = top + cellH

                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1.5f
                paint.color = Color.parseColor("#BFDBFE")
                // 底部稍微减去 2f，防止边框贴死屏幕边缘被切
                rectF.set(left + 2f, top + 2f, right - 2f, bottom - 2f) 
                canvas.drawRoundRect(rectF, 10f, 10f, paint)

                if (isToday) {
                    paint.style = Paint.Style.FILL
                    paint.color = Color.parseColor("#DBEAFE")
                    canvas.drawRoundRect(rectF, 10f, 10f, paint)
                }

                textPaint.textAlign = Paint.Align.LEFT
                textPaint.textSize = Math.min(cellW * 0.28f, cellH * 0.28f)
                if (textPaint.textSize > 32f) textPaint.textSize = 32f
                textPaint.color = Color.parseColor("#1E293B")
                textPaint.isFakeBoldText = true
                // Y 坐标稍微上提，避免压到底部
                canvas.drawText(d.toString(), left + 10f, top + 8f + textPaint.textSize, textPaint)

                val holiday = LunarCalc.getHoliday(date)
                val lunar = LunarCalc.lunarShort(date)
                textPaint.textAlign = Paint.Align.RIGHT
                textPaint.textSize = Math.min(cellW * 0.2f, cellH * 0.2f)
                if (textPaint.textSize > 24f) textPaint.textSize = 24f
                textPaint.isFakeBoldText = false
                textPaint.color = Color.parseColor(if (holiday != null) "#EF4444" else "#94A3B8")
                canvas.drawText(if (holiday != null) holiday else lunar, right - 10f, top + 8f + textPaint.textSize, textPaint)

                val shift = if (data != null && me != null) ShiftCalc.calcShift(data, me, date) else null
                val shiftStr = ShiftCalc.shiftShort(shift)
                if (shiftStr != "—") {
                    textPaint.textAlign = Paint.Align.CENTER
                    textPaint.textSize = Math.min(cellW * 0.45f, cellH * 0.4f)
                    if (textPaint.textSize > 40f) textPaint.textSize = 40f
                    textPaint.color = Color.parseColor(getShiftColor(shiftStr))
                    textPaint.isFakeBoldText = true
                    // 【核心修复】把班次文字上提一点点（0.3f 改 0.35f），保证到底部有缓冲，绝不切字
                    canvas.drawText(shiftStr, left + cellW / 2f, getCenteredY(bottom - cellH * 0.35f, textPaint), textPaint)
                }
            }
        }
        return bitmap
    }
    
    private fun getShiftColor(shiftStr: String): String = when(shiftStr) { 
        "休" -> "#EF4444"
        "白" -> "#3B82F6"
        "夜" -> "#1E293B"
        "二" -> "#10B981" 
        else -> "#F59E0B" 
    }
}
