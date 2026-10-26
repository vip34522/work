package com.example.schedulewidget

import android.graphics.*
import org.json.JSONObject
import java.util.*

object MonthBitmapGenerator {

    fun generateMonthBitmap(width: Int, height: Int, title: String, data: JSONObject?, me: JSONObject?, offset: Int): Bitmap {
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
        val radius = 20f
        val borderW = 4f
        val headerH = Math.min(h * 0.12f, 70f)
        val paddingX = w * 0.04f

        fun getCenteredY(centerY: Float, paint: Paint): Float {
            val fm = paint.fontMetrics
            return centerY - (fm.ascent + fm.descent) / 2f
        }

        // 背景与圆角
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        canvas.drawRoundRect(0f, 0f, w, h, radius, radius, paint)

        val pathHeader = Path()
        pathHeader.addRoundRect(RectF(0f, 0f, w, headerH), floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f), Path.Direction.CW)
        paint.color = Color.parseColor("#5C8DF6")
        canvas.drawPath(pathHeader, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = borderW
        paint.color = Color.parseColor("#5C8DF6")
        canvas.drawRoundRect(borderW/2, borderW/2, w - borderW/2, h - borderW/2, radius, radius, paint)

        // 顶部文字和图标
        textPaint.color = Color.WHITE
        textPaint.textSize = headerH * 0.4f
        textPaint.isFakeBoldText = true
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(title, paddingX, getCenteredY(headerH / 2f, textPaint), textPaint)

        // 顶部中间大标题 (如 2026年10月)
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = headerH * 0.4f
        canvas.drawText("${year}年${month}月", w / 2f, getCenteredY(headerH / 2f, textPaint), textPaint)

        // 画图标占位
        val iconSize = headerH * 0.6f
        val iconY = headerH / 2f
        val nextX = w - paddingX - iconSize / 2f
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#40FFFFFF")
        rectF.set(nextX - iconSize/2f, iconY - iconSize/2f, nextX + iconSize/2f, iconY + iconSize/2f)
        canvas.drawRoundRect(rectF, 8f, 8f, paint)
        val prevX = nextX - iconSize * 1.5f
        rectF.set(prevX - iconSize/2f, iconY - iconSize/2f, prevX + iconSize/2f, iconY + iconSize/2f)
        canvas.drawRoundRect(rectF, 8f, 8f, paint)
        // 只需画三角形箭头即可
        val pathRight = Path().apply { moveTo(nextX - 10f, iconY - 12f); lineTo(nextX + 10f, iconY); lineTo(nextX - 10f, iconY + 12f); close() }
        paint.color = Color.WHITE
        canvas.drawPath(pathRight, paint)
        val pathLeft = Path().apply { moveTo(prevX + 10f, iconY - 12f); lineTo(prevX - 10f, iconY); lineTo(prevX + 10f, iconY + 12f); close() }
        canvas.drawPath(pathLeft, paint)

        // 画星期表头
        val weekNames = listOf("一", "二", "三", "四", "五", "六", "日")
        val contentW = w - paddingX * 2
        val cellW = contentW / 7f
        val weekHeaderH = h * 0.08f
        textPaint.textSize = weekHeaderH * 0.6f
        textPaint.color = Color.parseColor("#64748B")
        textPaint.isFakeBoldText = true

        for (i in 0 until 7) {
            val cx = paddingX + cellW * i + cellW / 2f
            canvas.drawText(weekNames[i], cx, getCenteredY(headerH + weekHeaderH / 2f, textPaint), textPaint)
        }

        // 画日历网格
        val gridStartY = headerH + weekHeaderH
        val gridHeight = h - gridStartY - 10f
        val rows = 6 // 固定画6行
        val cellH = gridHeight / rows

        val firstDay = Calendar.getInstance().apply { set(year, month - 1, 1) }
        val startPad = if (firstDay.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) 6 else firstDay.get(Calendar.DAY_OF_WEEK) - 2
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val todayCal = Calendar.getInstance()
        val isCurrentMonth = (todayCal.get(Calendar.YEAR) == year && todayCal.get(Calendar.MONTH) + 1 == month)
        val todayD = todayCal.get(Calendar.DAY_OF_MONTH)

        for (r in 0 until rows) {
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

                // 画边框 (浅蓝色)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 2f
                paint.color = Color.parseColor("#BFDBFE")
                rectF.set(left + 2f, top + 2f, right - 2f, bottom - 2f)
                canvas.drawRoundRect(rectF, 8f, 8f, paint)

                // 今天的高亮底色
                if (isToday) {
                    paint.style = Paint.Style.FILL
                    paint.color = Color.parseColor("#DBEAFE")
                    canvas.drawRoundRect(rectF, 8f, 8f, paint)
                }

                // 日期数字
                textPaint.textAlign = Paint.Align.LEFT
                textPaint.textSize = Math.min(cellW * 0.25f, 30f)
                textPaint.color = Color.parseColor("#1E293B")
                textPaint.isFakeBoldText = true
                canvas.drawText(d.toString(), left + 8f, top + 10f + textPaint.textSize, textPaint)

                // 农历或节日
                val holiday = LunarCalc.getHoliday(date)
                val lunar = LunarCalc.lunarShort(date)
                textPaint.textAlign = Paint.Align.RIGHT
                textPaint.textSize = Math.min(cellW * 0.18f, 22f)
                textPaint.isFakeBoldText = false
                textPaint.color = Color.parseColor(if (holiday != null) "#EF4444" else "#94A3B8")
                val lunarTxt = if (holiday != null) holiday else lunar
                canvas.drawText(lunarTxt, right - 8f, top + 10f + textPaint.textSize, textPaint)

                // 班次
                val shift = if (data != null && me != null) ShiftCalc.calcShift(data, me, date) else null
                val shiftStr = ShiftCalc.shiftShort(shift)
                if (shiftStr != "—") {
                    textPaint.textAlign = Paint.Align.CENTER
                    textPaint.textSize = Math.min(cellW * 0.4f, 40f)
                    textPaint.color = Color.parseColor(getShiftColor(shiftStr))
                    textPaint.isFakeBoldText = true
                    canvas.drawText(shiftStr, left + cellW / 2f, getCenteredY(bottom - cellH * 0.35f, textPaint), textPaint)
                }
            }
        }
        return bitmap
    }
    private fun getShiftColor(shiftStr: String): String = when(shiftStr) { "休" -> "#EF4444"; "白" -> "#3B82F6"; "夜" -> "#1E293B"; "二" -> "#0EA5E9"; else -> "#10B981" }
}
