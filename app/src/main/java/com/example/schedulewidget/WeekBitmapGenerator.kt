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

        val cellWidth = wFloat / 7f
        val topSectionHeight = hFloat * 0.45f

        // ================= 1. 画 7 天排班 =================
        for (i in 0 until 7) {
            val d = ShiftCalc.addDays(monday, i)
            val c = Calendar.getInstance().apply { time = d }
            val isToday = ShiftCalc.isToday(d)
            
            val shift = if (data != null && me != null) ShiftCalc.calcShift(data, me, d) else null
            val shiftStr = ShiftCalc.shiftShort(shift)
            val shiftColor = getShiftColor(shiftStr)

            val centerX = cellWidth * i.toFloat() + cellWidth / 2f
            val startY = (topSectionHeight - 110f) / 2f 

            if (isToday) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3f
                paint.color = Color.parseColor("#2563EB")
                rectF.set(cellWidth * i.toFloat() + 4f, startY - 5f, cellWidth * (i + 1).toFloat() - 4f, startY + 105f)
                canvas.drawRoundRect(rectF, 12f, 12f, paint)
            }

            // 画星期
            textPaint.color = Color.parseColor(if (isToday) "#2563EB" else "#64748B")
            textPaint.textSize = wFloat / 20f
            textPaint.isFakeBoldText = isToday
            canvas.drawText("周${weekNames[i]}", centerX, startY + 15f, textPaint)

            // 画日期
            textPaint.color = Color.parseColor("#1E293B")
            textPaint.textSize = wFloat / 15f
            textPaint.isFakeBoldText = true
            canvas.drawText(c.get(Calendar.DAY_OF_MONTH).toString(), centerX, startY + 50f, textPaint)

            // 画班次小标签
            val rectWidth = cellWidth * 0.8f
            val rectHeight = 32f
            val rectLeft = centerX - rectWidth / 2f
            val rectTop = startY + 65f
            val rectRight = centerX + rectWidth / 2f
            val rectBottom = rectTop + rectHeight

            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor(shiftColor)
            rectF.set(rectLeft, rectTop, rectRight, rectBottom)
            canvas.drawRoundRect(rectF, 10f, 10f, paint)

            textPaint.color = Color.WHITE
            textPaint.textSize = wFloat / 25f
            textPaint.isFakeBoldText = false
            val textY = rectTop + (rectHeight / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
            canvas.drawText(shiftStr, centerX, textY, textPaint)
        }

        // ================= 2. 画中间蓝色分割线 =================
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#5C8DF6")
        canvas.drawRect(0f, topSectionHeight, wFloat, topSectionHeight + 3f, paint)

        // ================= 3. 画底部详情 =================
        val bottomStartY = topSectionHeight + 3f
        val bottomHeight = hFloat - bottomStartY
        val bottomCenterY = bottomStartY + bottomHeight / 2f

        val leftColWidth = wFloat * 0.3f 
        val leftCenterX = leftColWidth / 2f

        // 3.1 左侧日期
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = Color.parseColor("#1E293B")
        textPaint.isFakeBoldText = true
        textPaint.textSize = wFloat / 14f 
        val dateStr = "${Calendar.getInstance().get(Calendar.MONTH) + 1}月${Calendar.getInstance().get(Calendar.DAY_OF_MONTH)}日"
        canvas.drawText(dateStr, leftCenterX, bottomCenterY - 20f, textPaint)
        
        // 农历与星期
        textPaint.textSize = wFloat / 22f
        textPaint.color = Color.parseColor("#64748B")
        textPaint.isFakeBoldText = false
        val lunar = LunarCalc.lunarShort(today)
        val weekName = weekNames[Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1]
        canvas.drawText("$lunar 星期$weekName", leftCenterX, bottomCenterY + 15f, textPaint)

        // 3.2 中间灰色竖线
        paint.color = Color.parseColor("#CBD5E1")
        paint.strokeWidth = 2f
        canvas.drawLine(leftColWidth, bottomStartY + 10f, leftColWidth, hFloat - 10f, paint)

        // 3.3 右侧人员列表（带班次标签）
        val listStartX = leftColWidth + 15f
        val personsToShow = getTodayPersons(data)
        
        val showCount = Math.min(personsToShow.size, 3) 
        var listY = bottomCenterY - (showCount * 40f) / 2f + 10f

        var count = 0
        for (p in personsToShow) {
            if (count >= 3) break
            
            val pName = p.optString("name")
            val shift = ShiftCalc.calcShift(data!!, p, today)
            val shiftStr = ShiftCalc.shiftShort(shift)
            val typeName = getTypeName(data, p.optString("shiftTypeId"))
            
            // 1. 画姓名
            textPaint.textAlign = Paint.Align.LEFT
            textPaint.color = Color.parseColor("#2563EB")
            textPaint.textSize = wFloat / 18f
            textPaint.isFakeBoldText = true
            canvas.drawText(pName, listStartX, listY, textPaint)
            
            // 2. 画班种（灰色）
            textPaint.color = Color.parseColor("#64748B")
            textPaint.isFakeBoldText = false
            textPaint.textSize = wFloat / 22f
            val typeX = listStartX + (wFloat / 7.5f) // 控制班种的起始位置
            canvas.drawText(typeName, typeX, listY, textPaint)
            
            // 3. 画班次小色块
            val shiftColor = getShiftColor(shiftStr)
            val badgeWidth = wFloat / 12f
            val badgeHeight = 24f
            val badgeLeft = typeX + (wFloat / 8f) // 色块紧跟班种
            val badgeTop = listY - badgeHeight + 6f
            val badgeRight = badgeLeft + badgeWidth
            val badgeBottom = listY + 6f
            
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor(shiftColor)
            rectF.set(badgeLeft, badgeTop, badgeRight, badgeBottom)
            canvas.drawRoundRect(rectF, 8f, 8f, paint)
            
            // 色块里的文字
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.color = Color.WHITE
            textPaint.textSize = wFloat / 24f
            val textCenterX = badgeLeft + badgeWidth / 2f
            val textCenterY = badgeTop + (badgeHeight / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
            canvas.drawText(shiftStr, textCenterX, textCenterY, textPaint)
            
            listY += 40f
            count++
        }
        
        if (personsToShow.size > 3) {
            textPaint.textAlign = Paint.Align.LEFT
            textPaint.color = Color.parseColor("#94A3B8")
            canvas.drawText("...", listStartX, listY, textPaint)
        }

        return bitmap
    }

    // 提取了统一的颜色获取方法
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
