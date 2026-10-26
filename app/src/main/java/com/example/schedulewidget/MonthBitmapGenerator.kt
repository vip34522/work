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
        val headerH = Math.max(h * 0.18f, 80f)
        val paddingX = w * 0.04f

        fun getCenteredY(centerY: Float, paint: Paint): Float {
            val fm = paint.fontMetrics
            return centerY - (fm.ascent + fm.descent) / 2f
        }

        // 背景
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

        // 【优化】顶部：标题在左，按钮包着日期在中间
        textPaint.color = Color.WHITE
        textPaint.textSize = Math.min(headerH * 0.3f, 36f)
        textPaint.isFakeBoldText = true
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(title, paddingX, getCenteredY(headerH / 2f, textPaint), textPaint)

        val dateStr = "${year}年${month}月"
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = Math.min(headerH * 0.28f, 32f) // 日期比名字稍小
        val dateW = textPaint.measureText(dateStr)
        val dateCenterX = w / 2f
        canvas.drawText(dateStr, dateCenterX, getCenteredY(headerH / 2f, textPaint), textPaint)

        // 绘制按钮
        val btnW = 48f
        val btnH = 36f
        val gap = 12f
        val iconY = headerH / 2f
        val prevX = dateCenterX - dateW / 2f - btnW / 2f - gap
        val nextX = dateCenterX + dateW / 2f + btnW / 2f + gap

        fun drawBtn(cx: Float, isPrev: Boolean) {
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#40FFFFFF")
            rectF.set(cx - btnW/2f, iconY - btnH/2f, cx + btnW/2f, iconY + btnH/2f)
            canvas.drawRoundRect(rectF, 8f, 8f, paint)
            val path = Path()
            if (isPrev) {
                path.moveTo(cx + 4f, iconY - 8f); path.lineTo(cx - 4f, iconY); path.lineTo(cx + 4f, iconY + 8f)
            } else {
                path.moveTo(cx - 4f, iconY - 8f); path.lineTo(cx + 4f, iconY); path.lineTo(cx - 4f, iconY + 8f)
            }
            path.close()
            paint.color = Color.WHITE
            canvas.drawPath(path, paint)
        }
        drawBtn(prevX, true)
        drawBtn(nextX, false)

        // 表格头部（星期）
        val weekNames = listOf("一", "二", "三", "四", "五", "六", "日")
        val contentW = w - paddingX * 2
        val cellW = contentW / 7f
        val weekHeaderH = h * 0.1f // 【优化】加高留白
        textPaint.textSize = Math.min(cellW * 0.35f, 28f)
        textPaint.color = Color.parseColor("#64748B")
        textPaint.isFakeBoldText = true

        for (i in 0 until 7) {
            val cx = paddingX + cellW * i + cellW / 2f
            canvas.drawText(weekNames[i], cx, getCenteredY(headerH + weekHeaderH / 2f, textPaint), textPaint)
        }

        // 画网格
        val gridStartY = headerH + weekHeaderH
        val gridHeight = h - gridStartY - 15f // 【优化】减小底部留白
        val rows = 6
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

                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 2f
                paint.color = Color.parseColor("#BFDBFE")
                rectF.set(left + 2f, top + 2f, right - 2f, bottom - 2f)
                canvas.drawRoundRect(rectF, 8f, 8f, paint)

                if (isToday) {
                    paint.style = Paint.Style.FILL
                    paint.color = Color.parseColor("#DBEAFE")
                    canvas.drawRoundRect(rectF, 8f, 8f, paint)
                }

                // 【核心修复】日期大小受格宽和格高的双重限制，防止拉宽变大
                textPaint.textAlign = Paint.Align.LEFT
                textPaint.textSize = Math.min(cellW * 0.25f, cellH * 0.25f) 
                if (textPaint.textSize > 28f) textPaint.textSize = 28f // 绝对上限
                textPaint.color = Color.parseColor("#1E293B")
                textPaint.isFakeBoldText = true
                canvas.drawText(d.toString(), left + 8f, top + 8f + textPaint.textSize, textPaint)

                val holiday = LunarCalc.getHoliday(date)
                val lunar = LunarCalc.lunarShort(date)
                textPaint.textAlign = Paint.Align.RIGHT
                textPaint.textSize = Math.min(cellW * 0.18f, cellH * 0.18f)
                if (textPaint.textSize > 22f) textPaint.textSize = 22f
                textPaint.isFakeBoldText = false
                textPaint.color = Color.parseColor(if (holiday != null) "#EF4444" else "#94A3B8")
                canvas.drawText(if (holiday != null) holiday else lunar, right - 8f, top + 8f + textPaint.textSize, textPaint)

                val shift = if (data != null && me != null) ShiftCalc.calcShift(data, me, date) else null
                val shiftStr = ShiftCalc.shiftShort(shift)
                if (shiftStr != "—") {
                    textPaint.textAlign = Paint.Align.CENTER
                    textPaint.textSize = Math.min(cellW * 0.4f, cellH * 0.4f)
                    if (textPaint.textSize > 40f) textPaint.textSize = 40f
                    textPaint.color = Color.parseColor(getShiftColor(shiftStr))
                    textPaint.isFakeBoldText = true
                    canvas.drawText(shiftStr, left + cellW / 2f, getCenteredY(bottom - cellH * 0.25f, textPaint), textPaint)
                }
            }
        }
        return bitmap
    }
    private fun getShiftColor(shiftStr: String): String = when(shiftStr) { "休" -> "#EF4444"; "白" -> "#3B82F6"; "夜" -> "#1E293B"; "二" -> "#0EA5E9"; else -> "#10B981" }
}
