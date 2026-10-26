package com.example.schedulewidget

import android.graphics.*
import org.json.JSONObject
import java.util.*

object WeekBitmapGenerator {

    fun generateWeekBitmap(width: Int, height: Int, title: String, data: JSONObject?, me: JSONObject?, offset: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val today = Date()
        val monday = ShiftCalc.addDays(ShiftCalc.mondayOf(today), offset * 7)
        val weekNames = listOf("一", "二", "三", "四", "五", "六", "日")

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rectF = RectF()

        val w = width.toFloat()
        val h = height.toFloat()
        val radius = 20f
        val borderW = 4f
        val headerH = Math.min(h * 0.2f, 80f)
        val paddingX = w * 0.05f

        fun getCenteredY(centerY: Float, paint: Paint): Float {
            val fm = paint.fontMetrics
            return centerY - (fm.ascent + fm.descent) / 2f
        }

        // 1. 画背景和边框
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

        // 2. 画顶部文字与按钮图标
        textPaint.color = Color.WHITE
        textPaint.textSize = headerH * 0.35f
        textPaint.isFakeBoldText = true
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(title, paddingX, getCenteredY(headerH / 2f, textPaint), textPaint)

        // 画右侧箭头图标（占位，真正点击在 XML）
        val iconSize = headerH * 0.4f
        val iconY = headerH / 2f
        // 右箭头
        val nextX = w - paddingX - iconSize / 2f
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#40FFFFFF")
        rectF.set(nextX - iconSize/2f, iconY - iconSize/2f, nextX + iconSize/2f, iconY + iconSize/2f)
        canvas.drawRoundRect(rectF, 8f, 8f, paint)
        // 绘制箭头路径
        val nextPath = Path().apply {
            moveTo(nextX - iconSize*0.15f, iconY - iconSize*0.2f)
            lineTo(nextX + iconSize*0.15f, iconY)
            lineTo(nextX - iconSize*0.15f, iconY + iconSize*0.2f)
            close()
        }
        paint.color = Color.WHITE
        canvas.drawPath(nextPath, paint)

        // 左箭头
        val prevX = nextX - iconSize * 1.5f
        paint.color = Color.parseColor("#40FFFFFF")
        rectF.set(prevX - iconSize/2f, iconY - iconSize/2f, prevX + iconSize/2f, iconY + iconSize/2f)
        canvas.drawRoundRect(rectF, 8f, 8f, paint)
        val prevPath = Path().apply {
            moveTo(prevX + iconSize*0.15f, iconY - iconSize*0.2f)
            lineTo(prevX - iconSize*0.15f, iconY)
            lineTo(prevX + iconSize*0.15f, iconY + iconSize*0.2f)
            close()
        }
        paint.color = Color.WHITE
        canvas.drawPath(prevPath, paint)

        // 3. 画日历区
        val topH = headerH + (h - headerH) * 0.45f
        val contentW = w - paddingX * 2
        val cellW = contentW / 7f
        val areaH = topH - headerH
        val line1Y = headerH + areaH * 0.28f
        val line2Y = headerH + areaH * 0.6f
        val line3Y = headerH + areaH * 0.85f

        for (i in 0 until 7) {
            val d = ShiftCalc.addDays(monday, i)
            val c = Calendar.getInstance().apply { time = d }
            val isToday = ShiftCalc.isToday(d)
            val shift = if (data != null && me != null) ShiftCalc.calcShift(data, me, d) else null
            val shiftStr = ShiftCalc.shiftShort(shift)
            val shiftColor = getShiftColor(shiftStr)
            val startX = paddingX + cellW * i
            val centerX = startX + cellW / 2f

            if (isToday) {
                paint.style = Paint.Style.FILL
                paint.color = Color.parseColor("#DBEAFE")
                rectF.set(startX + 2f, line1Y - areaH*0.2f, startX + cellW - 2f, line3Y + areaH*0.25f)
                canvas.drawRoundRect(rectF, 10f, 10f, paint)
            }

            textPaint.textAlign = Paint.Align.CENTER
            textPaint.textSize = Math.min(cellW * 0.35f, 36f)
            textPaint.color = Color.parseColor(if (isToday) "#2563EB" else "#64748B")
            textPaint.isFakeBoldText = isToday
            canvas.drawText("周${weekNames[i]}", centerX, getCenteredY(line1Y, textPaint), textPaint)

            textPaint.textSize = Math.min(cellW * 0.55f, 60f)
            textPaint.color = Color.parseColor(if (isToday) "#2563EB" else "#1E293B")
            textPaint.isFakeBoldText = true
            canvas.drawText(c.get(Calendar.DAY_OF_MONTH).toString(), centerX, getCenteredY(line2Y, textPaint), textPaint)

            val rectW = cellW * 0.8f
            val rectH = Math.min(areaH * 0.2f, 38f)
            rectF.set(centerX - rectW / 2f, line3Y - rectH / 2f, centerX + rectW / 2f, line3Y + rectH / 2f)
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor(shiftColor)
            canvas.drawRoundRect(rectF, 8f, 8f, paint)

            textPaint.textSize = Math.min(rectH * 0.65f, 28f)
            textPaint.color = Color.WHITE
            textPaint.isFakeBoldText = false
            canvas.drawText(shiftStr, centerX, getCenteredY(line3Y, textPaint), textPaint)
        }

        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#E2E8F0")
        canvas.drawRect(paddingX, topH, w - paddingX, topH + 2f, paint)

        // 4. 画底部详情
        val botStartY = topH + 2f
        val botH = h - botStartY
        val leftW = contentW * 0.32f
        val leftCenterX = paddingX + leftW / 2f

        val dateItems = mutableListOf("${Calendar.getInstance().get(Calendar.MONTH) + 1}月${Calendar.getInstance().get(Calendar.DAY_OF_MONTH)}日", LunarCalc.lunarFull(today), "星期${weekNames[Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1]}")
        val dateColors = mutableListOf("#1E293B", "#64748B", "#64748B")
        val holiday = LunarCalc.getHoliday(today)
        if (!holiday.isNullOrEmpty()) {
            dateItems.add(holiday)
            dateColors.add("#EF4444")
        }

        val itemCount = dateItems.size
        val textRowH = Math.min(botH / 4.5f, 45f)
        var currentY = botStartY + (botH - itemCount * textRowH) / 2f + textRowH / 2f

        textPaint.textAlign = Paint.Align.CENTER
        for (i in 0 until itemCount) {
            textPaint.color = Color.parseColor(dateColors[i])
            textPaint.textSize = if (i == 0) Math.min(textRowH * 0.7f, 40f) else Math.min(textRowH * 0.5f, 30f)
            textPaint.isFakeBoldText = (i == 0 || i == 3)
            canvas.drawText(dateItems[i], leftCenterX, getCenteredY(currentY, textPaint), textPaint)
            currentY += textRowH
        }

        paint.color = Color.parseColor("#E2E8F0")
        paint.strokeWidth = 2f
        canvas.drawLine(paddingX + leftW, botStartY + 12f, paddingX + leftW, h - 15f, paint)

        val personsToShow = getTodayPersons(data)
        val showCount = Math.min(personsToShow.size, 3)
        val listRowH = Math.min(botH / 3.2f, 55f)
        val listStartY = botStartY + (botH - showCount * listRowH) / 2f + listRowH / 2f
        val rightStartX = paddingX + leftW + 12f
        val colNameX = rightStartX
        val colTypeX = rightStartX + (w * 0.20f)
        val colBadgeRight = w - paddingX

        var count = 0
        for (p in personsToShow) {
            if (count >= 3) break
            val pName = p.optString("name")
            val shift = ShiftCalc.calcShift(data!!, p, today)
            val shiftStr = ShiftCalc.shiftShort(shift)
            val typeName = getTypeName(data, p.optString("shiftTypeId"))
            val shiftColor = getShiftColor(shiftStr)
            val rowCenterY = listStartY + count * listRowH

            if (count > 0) {
                paint.color = Color.parseColor("#F1F5F9")
                paint.strokeWidth = 1.5f
                canvas.drawLine(rightStartX, rowCenterY - listRowH / 2f, w - paddingX, rowCenterY - listRowH / 2f, paint)
            }

            textPaint.textAlign = Paint.Align.LEFT
            textPaint.color = Color.parseColor("#2563EB")
            textPaint.isFakeBoldText = true
            textPaint.textSize = Math.min(listRowH * 0.45f, 36f)
            canvas.drawText(pName, colNameX, getCenteredY(rowCenterY, textPaint), textPaint)

            textPaint.color = Color.parseColor("#64748B")
            textPaint.isFakeBoldText = false
            textPaint.textSize = Math.min(listRowH * 0.35f, 26f)
            canvas.drawText(typeName, colTypeX, getCenteredY(rowCenterY, textPaint), textPaint)

            val badgeW = Math.min(w * 0.12f, 45f)
            val badgeH = Math.min(listRowH * 0.6f, 32f)
            val badgeLeft = colBadgeRight - badgeW
            rectF.set(badgeLeft, rowCenterY - badgeH / 2f, colBadgeRight, rowCenterY + badgeH / 2f)
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor(shiftColor)
            canvas.drawRoundRect(rectF, 6f, 6f, paint)

            textPaint.textAlign = Paint.Align.CENTER
            textPaint.color = Color.WHITE
            textPaint.textSize = Math.min(badgeH * 0.65f, 24f)
            canvas.drawText(shiftStr, badgeLeft + badgeW / 2f, getCenteredY(rowCenterY, textPaint), textPaint)
            count++
        }
        return bitmap
    }
    // ... getShiftColor, getTypeName, getTodayPersons 保持不变
    private fun getShiftColor(shiftStr: String): String = when(shiftStr) { "休" -> "#EF4444"; "白" -> "#3B82F6"; "夜" -> "#1E293B"; "二" -> "#0EA5E9"; else -> "#10B981" }
    private fun getTypeName(data: JSONObject, typeId: String): String {
        val types = data.optJSONArray("shiftTypes") ?: return "排班"
        for (i in 0 until types.length()) { if (types.getJSONObject(i).optString("id") == typeId) return types.getJSONObject(i).optString("name") }
        return "排班"
    }
    private fun getTodayPersons(data: JSONObject?): List<JSONObject> {
        val list = mutableListOf<JSONObject>()
        val arr = data?.optJSONArray("personnel") ?: return list
        for (i in 0 until arr.length()) list.add(arr.getJSONObject(i))
        return list
    }
}
