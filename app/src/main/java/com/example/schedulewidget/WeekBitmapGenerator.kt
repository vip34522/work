package com.example.schedulewidget

import android.graphics.*
import org.json.JSONObject
import java.util.*

object WeekBitmapGenerator {

    fun generateWeekBitmap(width: Int, height: Int, data: JSONObject?, me: JSONObject?, offset: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val today = Date()
        val monday = ShiftCalc.addDays(ShiftCalc.mondayOf(today), offset * 7)
        val weekNames = listOf("一", "二", "三", "四", "五", "六", "日")

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
        val rectF = RectF()

        val wFloat = width.toFloat()
        val hFloat = height.toFloat()

        // ================= 1. 画 7 天排班 (占上方 55% 高度) =================
        val topSectionHeight = hFloat * 0.55f
        val cellWidth = wFloat / 7f
        val dayCellHeight = topSectionHeight / 2f // 给上下留点空间

        for (i in 0 until 7) {
            val d = ShiftCalc.addDays(monday, i)
            val c = Calendar.getInstance().apply { time = d }
            val isToday = ShiftCalc.isToday(d)
            
            val shift = if (data != null && me != null) ShiftCalc.calcShift(data, me, d) else null
            val shiftStr = ShiftCalc.shiftShort(shift)
            val shiftColor = getShiftColor(shiftStr)

            val centerX = cellWidth * i.toFloat() + cellWidth / 2f
            // 让日历整体在 topSectionHeight 中垂直居中
            val startY = (topSectionHeight - dayCellHeight) / 2f 

            // 1.1 画今天的高亮外框
            if (isToday) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 4f
                paint.color = Color.parseColor("#2563EB")
                // 框住整个格子
                rectF.set(cellWidth * i.toFloat() + 2f, startY, cellWidth * (i + 1).toFloat() - 2f, startY + dayCellHeight)
                canvas.drawRoundRect(rectF, 12f, 12f, paint)
            }

            // 1.2 画星期几 (字号不超多宽度的 1/4)
            val textSizeWeek = Math.min(cellWidth / 4f, 30f)
            textPaint.color = Color.parseColor(if (isToday) "#2563EB" else "#64748B")
            textPaint.textSize = textSizeWeek
            textPaint.isFakeBoldText = isToday
            canvas.drawText("周${weekNames[i]}", centerX, startY + textSizeWeek, textPaint)

            // 1.3 画日期数字
            val textSizeDay = Math.min(cellWidth / 2.5f, 50f)
            textPaint.color = Color.parseColor("#1E293B")
            textPaint.textSize = textSizeDay
            textPaint.isFakeBoldText = true
            canvas.drawText(c.get(Calendar.DAY_OF_MONTH).toString(), centerX, startY + textSizeWeek + textSizeDay + 5f, textPaint)

            // 1.4 画班次色块
            val rectWidth = cellWidth * 0.75f
            val rectHeight = Math.min(dayCellHeight * 0.35f, 40f)
            val rectLeft = centerX - rectWidth / 2f
            val rectTop = startY + dayCellHeight - rectHeight - 2f
            val rectRight = centerX + rectWidth / 2f
            val rectBottom = rectTop + rectHeight

            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor(shiftColor)
            rectF.set(rectLeft, rectTop, rectRight, rectBottom)
            canvas.drawRoundRect(rectF, 10f, 10f, paint)

            textPaint.color = Color.WHITE
            textPaint.textSize = Math.min(rectHeight * 0.6f, 26f)
            textPaint.isFakeBoldText = false
            val textY = rectTop + (rectHeight / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
            canvas.drawText(shiftStr, centerX, textY, textPaint)
        }

        // ================= 2. 画中间分割线 =================
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#5C8DF6")
        canvas.drawRect(0f, topSectionHeight, wFloat, topSectionHeight + 3f, paint)

        // ================= 3. 画底部详情 (占下方 45% 高度) =================
        val bottomStartY = topSectionHeight + 3f
        val bottomHeight = hFloat - bottomStartY
        
        val leftColWidth = wFloat * 0.32f 
        
        // 3.1 左侧日期区
        val dateCenterX = leftColWidth / 2f
        val dateCenterY = bottomStartY + bottomHeight / 2f

        textPaint.textAlign = Paint.Align.CENTER
        
        // 大日期
        textPaint.color = Color.parseColor("#1E293B")
        textPaint.isFakeBoldText = true
        textPaint.textSize = Math.min(wFloat / 14f, 48f)
        val dateStr = "${Calendar.getInstance().get(Calendar.MONTH) + 1}月${Calendar.getInstance().get(Calendar.DAY_OF_MONTH)}日"
        canvas.drawText(dateStr, dateCenterX, dateCenterY - 10f, textPaint)
        
        // 农历与星期
        textPaint.textSize = Math.min(wFloat / 24f, 26f)
        textPaint.color = Color.parseColor("#64748B")
        textPaint.isFakeBoldText = false
        val lunar = LunarCalc.lunarShort(today)
        val weekName = weekNames[Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1]
        canvas.drawText("$lunar 星期$weekName", dateCenterX, dateCenterY + 25f, textPaint)

        // 3.2 中间灰色竖线
        paint.color = Color.parseColor("#CBD5E1")
        paint.strokeWidth = 2f
        canvas.drawLine(leftColWidth, bottomStartY + 10f, leftColWidth, hFloat - 10f, paint)

        // 3.3 右侧人员列表（按行高动态计算）
        val listStartX = leftColWidth + 15f
        val personsToShow = getTodayPersons(data)
        
        val showCount = Math.min(personsToShow.size, 3) 
        val rowHeight = Math.min(bottomHeight / 3.5f, 50f) // 每行最大高度
        val listStartY = bottomStartY + (bottomHeight - (showCount * rowHeight)) / 2f + rowHeight / 2f // 整体居中

        var count = 0
        for (p in personsToShow) {
            if (count >= 3) break
            
            val pName = p.optString("name")
            val shift = ShiftCalc.calcShift(data!!, p, today)
            val shiftStr = ShiftCalc.shiftShort(shift)
            val typeName = getTypeName(data, p.optString("shiftTypeId"))
            
            val currentY = listStartY + count * rowHeight

            // 1. 画姓名
            textPaint.textAlign = Paint.Align.LEFT
            textPaint.color = Color.parseColor("#2563EB")
            textPaint.textSize = Math.min(rowHeight * 0.5f, 32f)
            textPaint.isFakeBoldText = true
            canvas.drawText(pName, listStartX, currentY, textPaint)
            
            // 2. 画班种
            textPaint.color = Color.parseColor("#64748B")
            textPaint.isFakeBoldText = false
            textPaint.textSize = Math.min(rowHeight * 0.4f, 26f)
            val typeX = listStartX + (wFloat / 6.5f)
            canvas.drawText(typeName, typeX, currentY, textPaint)
            
            // 3. 画班次小色块
            val shiftColor = getShiftColor(shiftStr)
            val badgeWidth = Math.min(wFloat / 10f, 60f)
            val badgeHeight = Math.min(rowHeight * 0.6f, 34f)
            val badgeLeft = typeX + (wFloat / 7f)
            // 让色块垂直居中于当前行
            val badgeTop = currentY - badgeHeight + (badgeHeight / 2f) + 5f 
            val badgeRight = badgeLeft + badgeWidth
            val badgeBottom = badgeTop + badgeHeight
            
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor(shiftColor)
            rectF.set(badgeLeft, badgeTop, badgeRight, badgeBottom)
            canvas.drawRoundRect(rectF, 8f, 8f, paint)
            
            // 色块里的文字
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.color = Color.WHITE
            textPaint.textSize = Math.min(badgeHeight * 0.6f, 26f)
            val textCenterX = badgeLeft + badgeWidth / 2f
            val textCenterY = badgeTop + (badgeHeight / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
            canvas.drawText(shiftStr, textCenterX, textCenterY, textPaint)
            
            count++
        }
        
        if (personsToShow.size > 3) {
            textPaint.textAlign = Paint.Align.LEFT
            textPaint.color = Color.parseColor("#94A3B8")
            canvas.drawText("...", listStartX, listStartY + 3 * rowHeight, textPaint)
        }

        return bitmap
    }

    private fun getShiftColor(shiftStr: String): String {
        return when(shiftStr) {
            "休" -> "#EF4444"
            "白" -> "#3B82F6"
            "夜" -> "#1E293B"
            "二" -> "#0EA5E9"
            else -> "#10B981"
        }
    }

    private fun getTypeName(data: JSONObject, typeId: String): String {
        val types = data.optJSONArray("shiftTypes") ?: return "排班"
        for (i in 0 until types.length()) {
            if (types.getJSONObject(i).optString("id") == typeId) {
                return types.getJSONObject(i).optString("name")
            }
        }
        return "排班"
    }
    
    private fun getTodayPersons(data: JSONObject?): List<JSONObject> {
        val list = mutableListOf<JSONObject>()
        val arr = data?.optJSONArray("personnel") ?: return list
        for (i in 0 until arr.length()) {
            list.add(arr.getJSONObject(i))
        }
        return list
    }
}
