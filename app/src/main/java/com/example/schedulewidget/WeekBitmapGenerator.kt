package com.example.schedulewidget

import android.graphics.*
import org.json.JSONObject
import java.util.*

object WeekBitmapGenerator {

    fun generateWeekBitmap(width: Int, height: Int, data: JSONObject?, me: JSONObject?, offset: Int): Bitmap {
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
        val paddingX = w * 0.05f

        fun getCenteredY(centerY: Float, paint: Paint): Float {
            val fm = paint.fontMetrics
            return centerY - (fm.ascent + fm.descent) / 2f
        }

        // 全白底（因为外层蓝条由XML画了，画布只要负责白底）
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, w, h, paint)

        // 画日历区 (上方占 50%)
        val topH = h * 0.50f
        val contentW = w - paddingX * 2
        val cellW = contentW / 7f
        
        val line1Y = topH * 0.25f
        val line2Y = topH * 0.55f
        val line3Y = topH * 0.82f

        for (i in 0 until 7) {
            val d = ShiftCalc.addDays(monday, i)
            val c = Calendar.getInstance().apply { time = d }
            val isToday = ShiftCalc.isToday(d)
            
            val shift = if (data != null && me != null) ShiftCalc.calcShift(data, me, d) else null
            val shiftStr = ShiftCalc.shiftShort(shift)
            val shiftColor = getShiftColor(shiftStr)

            val startX = paddingX + cellW * i
            val centerX = startX + cellW / 2f

            // 今日高亮
            if (isToday) {
                paint.style = Paint.Style.FILL
                paint.color = Color.parseColor("#DBEAFE")
                rectF.set(startX + 2f, line1Y - 20f, startX + cellW - 2f, line3Y + 22f)
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
            val rectH = Math.min(topH * 0.22f, 38f)
            rectF.set(centerX - rectW / 2f, line3Y - rectH / 2f, centerX + rectW / 2f, line3Y + rectH / 2f)
            
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor(shiftColor)
            canvas.drawRoundRect(rectF, 8f, 8f, paint)

            textPaint.textSize = Math.min(rectH * 0.65f, 28f)
            textPaint.color = Color.WHITE
            textPaint.isFakeBoldText = false
            canvas.drawText(shiftStr, centerX, getCenteredY(line3Y, textPaint), textPaint)
        }

        // 分割线
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#E2E8F0")
        canvas.drawRect(paddingX, topH, w - paddingX, topH + 2f, paint)

        // 画底部详情
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

        // 人员列表
        val personsToShow = getTodayPersons(data)
        val showCount = Math.min(personsToShow.size, 3)
        val listRowH = Math.min(botH / 3.2f, 55f)
        val listStartY = botStartY + (botH - showCount * listRowH) / 2f + listRowH / 2f
        val rightStartX = paddingX + leftW + 8f
        val colNameX = rightStartX
        val colTypeX = rightStartX + (w * 0.18f)
        val colBadgeRight = w - paddingX - 8f

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
            textPaint.textSize = Math.min(listRowH * 0.5f, 38f)
            canvas.drawText(pName, colNameX, getCenteredY(rowCenterY, textPaint), textPaint)

            textPaint.color = Color.parseColor("#64748B")
            textPaint.isFakeBoldText = false
            textPaint.textSize = Math.min(listRowH * 0.4f, 28f)
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

    private fun getShiftColor(shiftStr: String): String = when(shiftStr) { "休" -> "#EF4444"; "白" -> "#3B82F6"; "夜" -> "#1E293B"; "二" -> "#10B981"; else -> "#F59E0B" }
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
