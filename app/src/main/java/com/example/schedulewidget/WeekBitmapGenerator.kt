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
        
        // 定义画笔
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
        val rectF = RectF()

        val cellWidth = width / 7f
        val topSectionHeight = height * 0.45f // 上半部分留给日历

        // ================= 画 7 天排班 =================
        for (i in 0 until 7) {
            val d = ShiftCalc.addDays(monday, i)
            val c = Calendar.getInstance().apply { time = d }
            val isToday = ShiftCalc.isToday(d)
            
            val shift = if (data != null && me != null) ShiftCalc.calcShift(data, me, d) else null
            val shiftStr = ShiftCalc.shiftShort(shift)
            val shiftColor = when(shiftStr) {
                "休" -> "#EF4444"
                "白" -> "#3B82F6"
                "夜" -> "#1E293B"
                "二" -> "#0EA5E9"
                else -> "#10B981"
            }

            val centerX = cellWidth * i + cellWidth / 2
            var startY = 16f

            // 如果今天是当前格子，画蓝色高亮外框
            if (isToday) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 4f
                paint.color = Color.parseColor("#2563EB")
                rectF.set(cellWidth * i + 4f, 4f, cellWidth * (i + 1) - 4f, topSectionHeight - 10f)
                canvas.drawRoundRect(rectF, 16f, 16f, paint)
            }

            // 画星期
            textPaint.color = Color.parseColor(if (isToday) "#2563EB" else "#64748B")
            textPaint.textSize = 28f
            textPaint.isFakeBoldText = isToday
            canvas.drawText("周${weekNames[i]}", centerX, startY + 30f, textPaint)

            // 画日期
            textPaint.color = Color.parseColor("#1E293B")
            textPaint.textSize = 40f
            textPaint.isFakeBoldText = true
            canvas.drawText(c.get(Calendar.DAY_OF_MONTH).toString(), centerX, startY + 85f, textPaint)

            // 画班次小标签
            val rectWidth = cellWidth * 0.8f
            val rectHeight = 46f
            val rectLeft = centerX - rectWidth / 2
            val rectTop = startY + 115f
            val rectRight = centerX + rectWidth / 2
            val rectBottom = rectTop + rectHeight

            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor(shiftColor)
            rectF.set(rectLeft, rectTop, rectRight, rectBottom)
            canvas.drawRoundRect(rectF, 12f, 12f, paint)

            textPaint.color = Color.WHITE
            textPaint.textSize = 28f
            textPaint.isFakeBoldText = false
            val textY = rectTop + (rectHeight / 2) - ((textPaint.descent() + textPaint.ascent()) / 2)
            canvas.drawText(shiftStr, centerX, textY, textPaint)
        }

        // ================= 画中间分割线 =================
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#5C8DF6")
        canvas.drawRect(20f, topSectionHeight, width - 20f, topSectionHeight + 4f, paint)

        // ================= 画底部详情 =================
        val bottomStartY = topSectionHeight + 24f
        val leftColWidth = width * 0.25f

        // 1. 左侧日期
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = Color.parseColor("#1E293B")
        textPaint.isFakeBoldText = true
        
        textPaint.textSize = 32f
        canvas.drawText("${Calendar.getInstance().get(Calendar.MONTH) + 1}月${Calendar.getInstance().get(Calendar.DAY_OF_MONTH)}日", leftColWidth / 2, bottomStartY + 30f, textPaint)
        
        textPaint.textSize = 24f
        textPaint.color = Color.parseColor("#64748B")
        textPaint.isFakeBoldText = false
        val lunar = LunarCalc.lunarShort(today)
        val weekName = weekNames[Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1]
        canvas.drawText(lunar, leftColWidth / 2, bottomStartY + 65f, textPaint)
        canvas.drawText("星期$weekName", leftColWidth / 2, bottomStartY + 95f, textPaint)

        // 2. 中间的竖线
        paint.color = Color.parseColor("#CBD5E1")
        paint.strokeWidth = 2f
        canvas.drawLine(leftColWidth, bottomStartY, leftColWidth, height - 20f, paint)

        // 3. 右侧人员列表
        textPaint.textAlign = Paint.Align.LEFT
        val listStartX = leftColWidth + 20f
        
        // 模拟取前几个人（实际开发中这里可以遍历所有人员）
        var listY = bottomStartY + 10f
        val personsToShow = getTodayPersons(data) // 获取所有人员
        
        var count = 0
        for (p in personsToShow) {
            if (count >= 4) break // 最多画4个，防止放不下
            
            val pName = p.optString("name")
            val shift = ShiftCalc.calcShift(data!!, p, today)
            val shiftStr = ShiftCalc.shiftShort(shift)
            
            // 画名字（蓝色）
            textPaint.color = Color.parseColor("#2563EB")
            textPaint.textSize = 24f
            textPaint.isFakeBoldText = true
            canvas.drawText(pName, listStartX, listY, textPaint)
            
            // 画班次（灰色）
            textPaint.color = Color.parseColor("#64748B")
            textPaint.isFakeBoldText = false
            val typeName = getTypeName(data, p.optString("shiftTypeId"))
            canvas.drawText("$typeName  $shiftStr", listStartX + 90f, listY, textPaint)
            
            listY += 36f
            count++
        }
        
        // 如果人多，画一个【更多】
        if (personsToShow.size > 4) {
            textPaint.color = Color.parseColor("#94A3B8")
            canvas.drawText("【更多】", listStartX + 90f, listY, textPaint)
        }

        return bitmap
    }

    // 辅助方法：获取班组名称
    private fun getTypeName(data: JSONObject, typeId: String): String {
        val types = data.optJSONArray("shiftTypes") ?: return "排班"
        for (i in 0 until types.length()) {
            if (types.getJSONObject(i).optString("id") == typeId) {
                return types.getJSONObject(i).optString("name")
            }
        }
        return "排班"
    }
    
    // 辅助方法：获取人员列表
    private fun getTodayPersons(data: JSONObject?): List<JSONObject> {
        val list = mutableListOf<JSONObject>()
        val arr = data?.optJSONArray("personnel") ?: return list
        for (i in 0 until arr.length()) {
            list.add(arr.getJSONObject(i))
        }
        return list
    }
}
